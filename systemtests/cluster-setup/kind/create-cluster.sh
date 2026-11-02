#!/usr/bin/env bash
# Creates (or reuses) a local kind cluster with ports 80/443 mapped to the
# host, installs ingress-nginx bound via hostPort on the node, and verifies
# the whole exposure path actually works with a throwaway smoke test before
# declaring success.
#
# This is the macOS-safe alternative to minikube's ingress addon:
# Docker Desktop / Podman Desktop / Colima all run the container engine
# inside a VM, so a minikube node's IP (used by CI's `$(minikube ip).nip.io`)
# isn't routable from the host. kind's extraPortMappings + ingress-nginx
# hostPort sidesteps that by riding on the same host-port-forwarding Docker
# Desktop/Podman machine already do reliably. See
# ~/claude-docs/streamshub-local-sts/README.md for the full writeup and the
# fallback strategy if this ever stops working on a given machine.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./env.sh

check_linux_rootless_podman_ip_tables

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
# the ingress controller — without it, ingress-nginx's worker processes
# crash once a Kafka secure listener Ingress shows up (nginx.conf becomes
# invalid: "worker process exited with fatal code 2 and cannot be
# respawned").
if [ "$(kubectl get deployment -n ingress-nginx ingress-nginx-controller -ojson | \
        jq -r '.spec.template.spec.containers[0].args | index("--enable-ssl-passthrough")')" = "null" ]; then
  echo "Enabling SSL passthrough on ingress-nginx (required for Kafka's secure listener)..."
  kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
    --type='json' \
    -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
  kubectl -n ingress-nginx rollout status deployment/ingress-nginx-controller --timeout=120s
fi

# Lets anything running *inside* the cluster (Console, most notably — it
# connects to Kafka's ingress-exposed "secure" listener using this same
# CONSOLE_CLUSTER_DOMAIN) actually reach the ingress controller through it.
# See common.sh:configure_dns_split_horizon for the full "why".
configure_dns_split_horizon "${CONSOLE_CLUSTER_DOMAIN}" ingress-nginx ingress-nginx-controller

cat > "${CLUSTER_ENV_FILE}" <<EOF
export CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}
export KUBECONTEXT=${KIND_CONTEXT}
EOF

# --- Verify the exposure path actually works end to end --------------------
# If this fails, the cluster isn't actually usable yet — don't declare success.
echo ""
echo "Verifying exposure path with a smoke test..."
verify_ingress_exposure "${CONSOLE_CLUSTER_DOMAIN}" "${INGRESS_HTTP_PORT}" "${INGRESS_HTTPS_PORT}"
echo "Smoke test passed: ${CONTAINER_ENGINE} -> kind node -> ingress-nginx -> pod is working."

echo ""
echo "Cluster ready. To use it in this shell:"
echo "  source ${CLUSTER_ENV_FILE}"
echo ""
echo "CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}"
echo "kubectl context: ${KIND_CONTEXT}"
if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  echo "podman machine sizing: PODMAN_MACHINE_CPUS=${PODMAN_MACHINE_CPUS} PODMAN_MACHINE_MEMORY=${PODMAN_MACHINE_MEMORY}MB (only applied on first-time machine init)"
fi
