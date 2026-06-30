#!/usr/bin/env bash

set -euox pipefail

OLM_INSTALL_SCRIPT=$(mktemp)

# Keep track of the last run command line number on error
trap 'last_line=${LINENO}' ERR

cleanup_and_exit() {
    # Capture the exact exit code of the last executed command
    local exit_code=${?}

    echo "🧹 Cleaning up..."
    if [ -e ${OLM_INSTALL_SCRIPT} ] ; then
        rm -vf ${OLM_INSTALL_SCRIPT}
    fi

    if [ "${exit_code}" -ne 0 ]; then
        echo "❌ Failed with exit code ${exit_code} at line ${last_line}"
    fi

    kill $(jobs -p) 2>/dev/null || true
    exit "${exit_code}"
}

trap cleanup_and_exit EXIT

OLM_VERSION="v0.45.0"
OLM_SCRIPT_SHA256="1e8065cb503d2ee94ce82dd2591618022852f53a43df908b9f8c7d314cff3532"

IMAGE_REGISTRY='localhost:5000'
IMAGE_GROUP='streamshub'
IMAGE_PREFIX="${IMAGE_REGISTRY}/${IMAGE_GROUP}"

if [ -n "${MINIKUBE_PROFILE:-}" ] ; then
    MK_PROFILE_ARG="--profile=${MINIKUBE_PROFILE}"
else
    MK_PROFILE_ARG=""
fi

if ! minikube status ${MK_PROFILE_ARG} >/dev/null 2>&1 ; then
  minikube start ${MK_PROFILE_ARG} \
    --driver=${MINIKUBE_DRIVER:-kvm2} \
    --cpus=${MINIKUBE_CPU_COUNT:-6} \
    --memory=${MINIKUBE_MEMORY:-16384} \
    --container-runtime=${MINIKUBE_CONTAINER_RUNTIME:-docker} \
    --addons=registry,storage,ingress,ingress-dns,metrics-server \
    --insecure-registry="${IMAGE_REGISTRY},10.0.0.0/24" \
    --extra-config=kubeadm.ignore-preflight-errors=SystemVerification \
    --extra-config=apiserver.authorization-mode=RBAC,Node
fi

# Enable TLS/passthrough in Nginx -------------------------------
if [ "$(kubectl get deployment -n ingress-nginx ingress-nginx-controller -ojson | \
        jq -r '.spec.template.spec.containers[0].args | index("--enable-ssl-passthrough")')" == "null" ] ; then
  kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
   --type='json' \
   -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
fi

# Setup OLM -----------------------------------------------------
curl --proto "=https" -o ${OLM_INSTALL_SCRIPT} -sL \
  https://github.com/operator-framework/operator-lifecycle-manager/releases/download/${OLM_VERSION}/install.sh

echo "${OLM_SCRIPT_SHA256} ${OLM_INSTALL_SCRIPT}" | sha256sum --check --status || { echo "Hash verification failed"; exit 1; }
chmod +x ${OLM_INSTALL_SCRIPT}
${OLM_INSTALL_SCRIPT} ${OLM_VERSION} || true

# Build and push Console images ---------------------------------
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr '[:upper:]' '[:lower:]')

if which podman 1>/dev/null 2>&1 && which docker 1>/dev/null 2>&1 ; then
    if docker --version | grep "podman" ; then
        # podman masquerading as docker
        CONTAINER_RUNTIME=podman
    else
        # both present, prefer docker
        CONTAINER_RUNTIME=docker
    fi
elif which podman 1>/dev/null 2>&1 ; then
    CONTAINER_RUNTIME=podman
else
    CONTAINER_RUNTIME=docker
fi

if [ "${CONTAINER_RUNTIME}" == "podman" ] ; then
    SKOPEO_LOCAL="containers-storage:"
else
    SKOPEO_LOCAL="docker-daemon:"
fi

if [ -z "${PLATFORMS:-}" ] ; then
    if [ "${CONTAINER_RUNTIME}" == "podman" ] ; then
        PLATFORMS=$(podman info --format={{".Version.OsArch"}})
    else
        PLATFORMS=$(docker system info --format '{{.OSType}}/{{.Architecture}}' 2>/dev/null)
    fi
fi

mvn clean package -Pcontainer-image -B --no-transfer-progress -DskipTests \
 -Dquarkus.kubernetes.namespace='$${NAMESPACE}' \
 -Dcontainer-image.registry="${IMAGE_REGISTRY}" \
 -Dcontainer-image.group="${IMAGE_GROUP}" \
 -Dcontainer-image.tag=${PROJECT_VERSION} \
 -Dcontainer-image.push=false

./operator/bin/modify-bundle-metadata.sh \
  "VERSION=${PROJECT_VERSION}" \
  "SKOPEO_TRANSPORT=${SKOPEO_LOCAL}" \
  "PLATFORMS=${PLATFORMS}"

${CONTAINER_RUNTIME} build \
 -t ${IMAGE_PREFIX}/console-operator-bundle:${PROJECT_VERSION} \
 -f operator/target/bundle/streamshub-console-operator/bundle.Dockerfile \
 operator/target/bundle/streamshub-console-operator

./operator/bin/generate-catalog.sh ./operator/target/bundle/streamshub-console-operator true

${CONTAINER_RUNTIME} build \
 -t ${IMAGE_PREFIX}/console-operator-catalog:${PROJECT_VERSION} \
 -f operator/src/main/docker/catalog.Dockerfile \
 operator/

socat TCP-LISTEN:5000,reuseaddr,fork TCP:$(minikube ip ${MK_PROFILE_ARG}):5000 &

# Wait for socat to warm up
for i in $(seq 1 30); do
    nc -z localhost 5000 && break
    sleep 1
done

for img in console-api console-operator console-operator-bundle console-operator-catalog ; do
  # TODO: deeper evaluation of `minikube image load`
  skopeo copy --preserve-digests --dest-tls-verify=false \
    ${SKOPEO_LOCAL}${IMAGE_PREFIX}/${img}:${PROJECT_VERSION} \
    docker://${IMAGE_PREFIX}/${img}:${PROJECT_VERSION}
done
