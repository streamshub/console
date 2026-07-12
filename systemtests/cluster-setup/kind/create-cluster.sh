#!/usr/bin/env bash
# Creates (or reuses) a local kind cluster with ports 80/443 mapped to the
# host, installs ingress-nginx bound via hostPort on the node, and verifies
# the whole exposure path actually works with a throwaway smoke test before
# declaring success.
#
# This is the macOS-safe alternative to systemtests/scripts/setup-minikube.sh:
# Docker Desktop / Podman Desktop / Colima all run the container engine
# inside a VM, so a minikube node's IP (used by CI's `$(minikube ip).nip.io`)
# isn't routable from the host. kind's extraPortMappings + ingress-nginx
# hostPort sidesteps that by riding on the same host-port-forwarding Docker
# Desktop/Podman machine already do reliably. See
# ~/claude-docs/streamshub-local-sts/README.md for the full writeup and the
# fallback strategy if this ever stops working on a given machine.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib/env.sh

if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  ensure_podman_machine
fi

KIND_CONFIG=$(mktemp)

cat > "${KIND_CONFIG}" <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: ${CLUSTER_NAME}
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: ${INGRESS_HTTP_PORT}
        protocol: TCP
      - containerPort: 443
        hostPort: ${INGRESS_HTTPS_PORT}
        protocol: TCP
EOF

if kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"; then
  echo "kind cluster '${CLUSTER_NAME}' already exists, skipping create"
else
  echo "Creating kind cluster '${CLUSTER_NAME}' (engine=${CONTAINER_ENGINE}, ports ${INGRESS_HTTP_PORT}/${INGRESS_HTTPS_PORT})..."
  kind create cluster --config "${KIND_CONFIG}"
fi
rm -f "${KIND_CONFIG}"

kubectl config use-context "${KIND_CONTEXT}" >/dev/null
kubectl wait --for=condition=Ready node --all --timeout=120s

if kubectl get deployment -n ingress-nginx ingress-nginx-controller >/dev/null 2>&1; then
  echo "ingress-nginx already installed, skipping"
else
  echo "Installing ingress-nginx (kind provider manifest)..."
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
fi

echo "Waiting for ingress-nginx controller (first-time image pulls can take several minutes)..."
kubectl -n ingress-nginx wait --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=600s

# Strimzi's TLS-terminating "secure" Kafka listener needs SSL passthrough on
# the ingress controller (matches systemtests/scripts/setup-minikube.sh) —
# without it, ingress-nginx's worker processes crash once a Kafka secure
# listener Ingress shows up (nginx.conf becomes invalid: "worker process
# exited with fatal code 2 and cannot be respawned").
if [ "$(kubectl get deployment -n ingress-nginx ingress-nginx-controller -ojson | \
        jq -r '.spec.template.spec.containers[0].args | index("--enable-ssl-passthrough")')" = "null" ]; then
  echo "Enabling SSL passthrough on ingress-nginx (required for Kafka's secure listener)..."
  kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
    --type='json' \
    -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
  kubectl -n ingress-nginx rollout status deployment/ingress-nginx-controller --timeout=120s
fi

cat > "${CLUSTER_ENV_FILE}" <<EOF
export CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}
export KUBECONTEXT=${KIND_CONTEXT}
EOF

# --- Verify the exposure path actually works end to end --------------------
# Deploys a throwaway echo server behind an Ingress, curls it over HTTP and
# HTTPS, then cleans up. If this fails, the cluster isn't actually usable
# yet — don't declare success.
echo ""
echo "Verifying exposure path with a smoke test..."
SMOKE_NAMESPACE="default"
SMOKE_HOST="echo.${CONSOLE_CLUSTER_DOMAIN}"
SMOKE_MANIFEST=$(mktemp)
cleanup_smoke_test() {
  kubectl delete -f "${SMOKE_MANIFEST}" --ignore-not-found >/dev/null 2>&1 || true
  rm -f "${SMOKE_MANIFEST}"
}
trap cleanup_smoke_test EXIT

cat > "${SMOKE_MANIFEST}" <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-setup-smoke-test
  namespace: ${SMOKE_NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels: {app: cluster-setup-smoke-test}
  template:
    metadata:
      labels: {app: cluster-setup-smoke-test}
    spec:
      containers:
        - name: echo-server
          image: registry.k8s.io/e2e-test-images/agnhost:2.53
          args: ["netexec", "--http-port=8080"]
          ports: [{containerPort: 8080}]
---
apiVersion: v1
kind: Service
metadata:
  name: cluster-setup-smoke-test
  namespace: ${SMOKE_NAMESPACE}
spec:
  selector: {app: cluster-setup-smoke-test}
  ports: [{port: 80, targetPort: 8080}]
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cluster-setup-smoke-test
  namespace: ${SMOKE_NAMESPACE}
spec:
  ingressClassName: nginx
  rules:
    - host: ${SMOKE_HOST}
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: cluster-setup-smoke-test
                port: {number: 80}
EOF

# ingress-nginx reloads its config asynchronously, so a freshly-created
# Ingress can 404/503/refuse connections for up to ~30s before routing works.
wait_for_http() {
  local url="$1"
  local curl_opts="$2"
  local attempt
  for attempt in $(seq 1 30); do
    if curl -fsS ${curl_opts} --max-time 5 "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for ${url} to become reachable" >&2
  return 1
}

kubectl apply -f "${SMOKE_MANIFEST}"
kubectl wait --for=condition=ready pod -l app=cluster-setup-smoke-test --timeout=180s -n "${SMOKE_NAMESPACE}"

echo "Checking http://${SMOKE_HOST}/ (ingress-nginx reload can take up to ~30s)..."
wait_for_http "http://${SMOKE_HOST}/" ""
echo "Checking https://${SMOKE_HOST}/ ..."
wait_for_http "https://${SMOKE_HOST}/" "-k"
echo "Smoke test passed: macOS -> ${CONTAINER_ENGINE} VM -> kind node -> ingress-nginx -> pod is working."

cleanup_smoke_test
trap - EXIT

echo ""
echo "Cluster ready. To use it in this shell:"
echo "  source ${CLUSTER_ENV_FILE}"
echo ""
echo "CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}"
echo "kubectl context: ${KIND_CONTEXT}"
if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  echo "podman machine sizing: PODMAN_MACHINE_CPUS=${PODMAN_MACHINE_CPUS} PODMAN_MACHINE_MEMORY=${PODMAN_MACHINE_MEMORY}MB (only applied on first-time machine init)"
fi
