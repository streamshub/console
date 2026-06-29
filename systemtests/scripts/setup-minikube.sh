#!/usr/bin/env bash

set -euox pipefail

SOCAT_PID=""

function handle_error {
    echo "Error occurred at line $1"
    if [ -n "${SOCAT_PID}" ] ; then
        kill ${SOCAT_PID}
    fi
    kill 0 # kill the master shell and all subshells
}

trap 'handle_error $LINENO' ERR

OLM_VERSION="v0.42.0"
OLM_SCRIPT_SHA256="1e8065cb503d2ee94ce82dd2591618022852f53a43df908b9f8c7d314cff3532"

if [ -n "${MINIKUBE_PROFILE}" ] ; then
    MK_PROFILE_ARG="--profile=${MINIKUBE_PROFILE}"
else
    MK_PROFILE_ARG=""
fi

if ! minikube status ${MK_PROFILE_ARG} ; then
  minikube start ${MK_PROFILE_ARG} \
    --driver=${MINIKUBE_DRIVER:-kvm2} \
    --cpus=${MINIKUBE_CPU_COUNT:-6} \
    --memory=${MINIKUBE_MEMORY:-16384} \
    --container-runtime=${MINIKUBE_CONTAINER_RUNTIME:-docker} \
    --addons=registry,storage,ingress,ingress-dns,metrics-server \
    --insecure-registry="localhost:5000,10.0.0.0/24" \
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
OLM_INSTALL_SCRIPT=$(mktemp)

curl --proto "=https" --tlsv1.3 -o ${OLM_INSTALL_SCRIPT} -sL \
  https://github.com/operator-framework/operator-lifecycle-manager/releases/download/${OLM_VERSION}/install.sh

echo "${OLM_SCRIPT_SHA256} ${OLM_INSTALL_SCRIPT}" | sha256sum --check --status || { echo "Hash verification failed"; exit 1; }
chmod +x ${OLM_INSTALL_SCRIPT}
${OLM_INSTALL_SCRIPT} ${OLM_VERSION} || true
rm -v ${OLM_INSTALL_SCRIPT}

# Build and push Console images ---------------------------------
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr '[:upper:]' '[:lower:]')
CONTAINER_RUNTIME=$(which podman || which docker)
SKOPEO_LOCAL=$(which podman >/dev/null && echo "containers-storage:" || echo "docker-daemon:")
PLATFORMS=$(docker system info --format '{{.OSType}}/{{.Architecture}}' 2>/dev/null || podman info --format={{".Version.OsArch"}})

mvn clean package -Pcontainer-image -B --no-transfer-progress -DskipTests \
 -Dquarkus.kubernetes.namespace='$${NAMESPACE}' \
 -Dcontainer-image.tag=${PROJECT_VERSION} \
 -Dcontainer-image.registry='localhost:5000' \
 -Dcontainer-image.push=false

./operator/bin/modify-bundle-metadata.sh \
  "VERSION=${PROJECT_VERSION}" \
  "SKOPEO_TRANSPORT=${SKOPEO_LOCAL}" \
  "PLATFORMS=${PLATFORMS}"

${CONTAINER_RUNTIME} build \
 -t localhost:5000/streamshub/console-operator-bundle:${PROJECT_VERSION} \
 -f operator/target/bundle/streamshub-console-operator/bundle.Dockerfile

./operator/bin/generate-catalog.sh ./operator/target/bundle/streamshub-console-operator true

${CONTAINER_RUNTIME} build \
 -t localhost:5000/streamshub/console-operator-catalog:${PROJECT_VERSION} \
 -f operator/src/main/docker/catalog.Dockerfile \
 operator/

socat TCP-LISTEN:5000,reuseaddr,fork TCP:$(minikube ip ${MK_PROFILE_ARG}):5000 &
SOCAT_PID=${!}
# Wait for socat to warm up
sleep 5

for img in console-api console-operator console-operator-bundle console-operator-catalog ; do
  skopeo copy --preserve-digests --dest-tls-verify=false \
    ${SKOPEO_LOCAL}localhost:5000/streamshub/${img}:${PROJECT_VERSION} \
    docker://localhost:5000/streamshub/${img}:${PROJECT_VERSION}
done

kill ${SOCAT_PID}
SOCAT_PID=""
