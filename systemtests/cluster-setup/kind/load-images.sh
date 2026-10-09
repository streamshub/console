#!/usr/bin/env bash
# Pushes the images built by ../common/build-console.sh to the local
# registry set up by ./setup-registry.sh, via skopeo (same mechanism
# systemtests/scripts/setup-minikube.sh already uses, parameterized here).
#
# Why skopeo and not `docker push`/`podman push`: the registry is plain
# HTTP, and getting a container engine to trust that for an arbitrary local
# hostname means either global daemon config (insecure-registries /
# registries.conf) or doesn't work at all depending on the engine/backend
# in use. skopeo's --dest-tls-verify=false sidesteps that per-invocation,
# no daemon-wide trust configuration needed.
#
# This has to be a real push, not a `kind load docker-image` straight into
# containerd: OLM's CatalogSource controller hardcodes
# `imagePullPolicy: Always` on its registry pod, so kubelet always does a
# real pull for the catalog image — it has to actually be resolvable from
# inside the cluster, not just sitting in containerd under the right name.
#
# Usage:
#   ./load-images.sh [--registry localhost:5000] [--group streamshub] [--tag <tag>]
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib/env.sh

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

if [ -z "${TAG}" ]; then
  TAG=$(cd ../../.. && mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr '[:upper:]' '[:lower:]')
fi

IMAGE_PREFIX="${REGISTRY}/${GROUP}"

# Same runtime detection as build-console.sh, to pick the matching skopeo
# source transport for wherever the images were actually built.
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

echo "Pushing images to ${REGISTRY} for kind cluster '${CLUSTER_NAME}'..."
for img in console-api console-operator console-operator-bundle console-operator-catalog; do
  echo "  ${IMAGE_PREFIX}/${img}:${TAG}"
  # --preserve-digests matters: without it, skopeo re-serializes the
  # manifest on copy and the digest changes, which breaks the
  # console-operator/console-api image references modify-bundle-metadata.sh
  # already baked into the CSV by digest.
  skopeo copy --preserve-digests --dest-tls-verify=false \
    "${SKOPEO_TRANSPORT}${IMAGE_PREFIX}/${img}:${TAG}" \
    "docker://${IMAGE_PREFIX}/${img}:${TAG}"
done

echo ""
echo "Done. Catalog image for setup-catalogsource.sh / deploy-example-console.sh:"
echo "  ${IMAGE_PREFIX}/console-operator-catalog:${TAG}"
