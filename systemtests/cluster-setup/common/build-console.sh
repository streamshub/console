#!/usr/bin/env bash
# Builds the Console images (API, operator, OLM bundle, OLM catalog) from
# local source. Local only — nothing is pushed anywhere. Splits the "build"
# half of systemtests/scripts/setup-minikube.sh out from the "get it into a
# cluster" half, which is now ../kind/load-images.sh / ../minikube/load-images.sh.
#
# Cluster-agnostic — doesn't touch kubectl or any cluster context.
#
# Usage:
#   ./build-console.sh [--registry localhost:5000] [--group streamshub] [--tag <tag>]
#
# Builds (tagged, never pushed):
#   <registry>/<group>/console-api:<tag>
#   <registry>/<group>/console-operator:<tag>
#   <registry>/<group>/console-operator-bundle:<tag>
#   <registry>/<group>/console-operator-catalog:<tag>
#
# <tag> defaults to the Maven project version, lowercased (matches the
# Makefile's own SNAPSHOT -> snapshot convention for container tags).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
PROJECT_ROOT="$(cd ../../.. && pwd)"

REGISTRY="${IMAGE_REGISTRY:-localhost:5000}"
GROUP="${IMAGE_GROUP:-streamshub}"
TAG="${IMAGE_TAG:-}"

usage() {
  echo "Usage: $0 [--registry localhost:5000] [--group streamshub] [--tag <tag>]" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --registry) REGISTRY="$2"; shift 2 ;;
    --group) GROUP="$2"; shift 2 ;;
    --tag) TAG="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown argument: $1" >&2; usage ;;
  esac
done

cd "${PROJECT_ROOT}"

if [ -z "${TAG}" ]; then
  TAG=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr '[:upper:]' '[:lower:]')
fi

IMAGE_PREFIX="${REGISTRY}/${GROUP}"

# --- Detect container runtime (prefer docker; podman can masquerade as it) -
if command -v podman >/dev/null 2>&1 && command -v docker >/dev/null 2>&1; then
  if docker --version | grep -q podman; then
    CONTAINER_RUNTIME=podman
  else
    CONTAINER_RUNTIME=docker
  fi
elif command -v podman >/dev/null 2>&1; then
  CONTAINER_RUNTIME=podman
else
  CONTAINER_RUNTIME=docker
fi

if [ "${CONTAINER_RUNTIME}" = "podman" ]; then
  SKOPEO_TRANSPORT="containers-storage:"
else
  SKOPEO_TRANSPORT="docker-daemon:"
fi

if [ -z "${PLATFORMS:-}" ]; then
  if [ "${CONTAINER_RUNTIME}" = "podman" ]; then
    PLATFORMS=$(podman info --format '{{.Version.OsArch}}')
  else
    PLATFORMS=$(docker system info --format '{{.OSType}}/{{.Architecture}}' 2>/dev/null)
  fi
fi

# On a docker CLI whose active buildx builder uses the "docker-container"
# driver (common once you've set up multi-platform builds), a plain
# `docker build` only populates buildx's own cache, not the local image
# store — it prints "No output specified ... use --load" and silently
# leaves nothing for skopeo/docker-inspect to find afterwards. Quarkus's
# docker extension only adds `--load` itself when quarkus.docker.buildx.platform
# is explicitly set to a single platform, so pass ours through to get that
# behavior reliably. Not applicable (and rejected outright) when podman is
# the detected runtime - buildx is docker-only.
BUILDX_PLATFORM_ARGS=()
if [ "${CONTAINER_RUNTIME}" = "docker" ]; then
  BUILDX_PLATFORM_ARGS=(-Dquarkus.docker.buildx.platform="${PLATFORMS}")
fi

echo "Building console-api / console-operator images (runtime=${CONTAINER_RUNTIME}, registry=${REGISTRY}, group=${GROUP}, tag=${TAG})..."
mvn clean package -Pcontainer-image -B --no-transfer-progress -DskipTests -DskipITs \
  -Dquarkus.kubernetes.namespace='$${NAMESPACE}' \
  -Dcontainer-image.registry="${REGISTRY}" \
  -Dcontainer-image.group="${GROUP}" \
  -Dcontainer-image.tag="${TAG}" \
  -Dcontainer-image.push=false \
  "${BUILDX_PLATFORM_ARGS[@]}"

echo "Building OLM bundle metadata..."
./operator/bin/modify-bundle-metadata.sh \
  "VERSION=${TAG}" \
  "SKOPEO_TRANSPORT=${SKOPEO_TRANSPORT}" \
  "PLATFORMS=${PLATFORMS}"

# Same --load reasoning as above, for our own direct docker build calls
# (podman build always writes to local storage directly, no such flag).
BUILD_LOAD_ARGS=()
if [ "${CONTAINER_RUNTIME}" = "docker" ]; then
  BUILD_LOAD_ARGS=(--load)
fi

echo "Building console-operator-bundle image..."
"${CONTAINER_RUNTIME}" build \
  "${BUILD_LOAD_ARGS[@]}" \
  -t "${IMAGE_PREFIX}/console-operator-bundle:${TAG}" \
  -f operator/target/bundle/streamshub-console-operator/bundle.Dockerfile \
  operator/target/bundle/streamshub-console-operator

echo "Generating OLM catalog..."
./operator/bin/generate-catalog.sh ./operator/target/bundle/streamshub-console-operator true

echo "Building console-operator-catalog image..."
"${CONTAINER_RUNTIME}" build \
  "${BUILD_LOAD_ARGS[@]}" \
  -t "${IMAGE_PREFIX}/console-operator-catalog:${TAG}" \
  -f operator/src/main/docker/catalog.Dockerfile \
  operator/

echo ""
echo "Built images (local only, not pushed):"
for img in console-api console-operator console-operator-bundle console-operator-catalog; do
  echo "  ${IMAGE_PREFIX}/${img}:${TAG}"
done
echo ""
echo "Next, load them into your cluster:"
echo "  ../kind/load-images.sh --registry ${REGISTRY} --group ${GROUP} --tag ${TAG}"
echo "  ../minikube/load-images.sh --registry ${REGISTRY} --group ${GROUP} --tag ${TAG}"
echo ""
echo "Then point setup-catalogsource.sh / deploy-example-console.sh at:"
echo "  ${IMAGE_PREFIX}/console-operator-catalog:${TAG}"
