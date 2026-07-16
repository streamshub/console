#!/usr/bin/env bash
# Creates (or reuses) a local minikube cluster with the ingress addon,
# exposes ingress-nginx to the host via a persistent `kubectl port-forward`
# on non-privileged local ports (default 8080/8443, no sudo needed) unless
# a tunnel (see enable-tunnel.sh) is already active, and verifies the whole
# exposure path actually works with a throwaway smoke test before declaring
# success.
#
# Why not the same approach as ../kind/ ? Confirmed empirically on this
# machine:
#   - minikube's ingress addon uses a NodePort Service, not hostPort
#     binding like kind's ingress-nginx does. Direct NodePort access from
#     macOS to the minikube node IP (e.g. curl 192.168.49.2:30674) times
#     out — matches long-standing upstream minikube/docker-driver-on-macOS
#     issues, reproduced here, not just a theoretical concern.
#   - `minikube tunnel` CAN make ingress-nginx reachable portlessly at
#     127.0.0.1 (patch the Service to type LoadBalancer first — tunnel only
#     manages LoadBalancer services, not NodePort), but binding ports <1024
#     requires sudo. See enable-tunnel.sh for that path if you need
#     portless URLs (e.g. for OIDC/auth redirect flows).
#
# See ~/claude-docs/streamshub-local-sts/README.md for the full
# investigation and the original decision to build ../kind/ first.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib/env.sh

check_linux_rootless_podman_ip_tables

if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  ensure_podman_machine
fi

if minikube status -p "${MINIKUBE_PROFILE}" >/dev/null 2>&1; then
  echo "minikube profile '${MINIKUBE_PROFILE}' already running, skipping create"
else
  echo "Creating minikube cluster '${MINIKUBE_PROFILE}' (driver=${CONTAINER_ENGINE})..."
  minikube start -p "${MINIKUBE_PROFILE}" \
    --driver="${CONTAINER_ENGINE}" \
    --addons=ingress
fi

kubectl config use-context "${MINIKUBE_PROFILE}" >/dev/null

echo "Waiting for ingress-nginx controller (first-time image pulls can take several minutes)..."
kubectl -n ingress-nginx wait --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=600s

if [ "$(kubectl get deployment -n ingress-nginx ingress-nginx-controller -ojson | \
        jq -r '.spec.template.spec.containers[0].args | index("--enable-ssl-passthrough")')" = "null" ]; then
  echo "Enabling SSL passthrough on ingress-nginx (required for Kafka's secure listener)..."
  kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
    --type='json' \
    -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
  kubectl -n ingress-nginx rollout status deployment/ingress-nginx-controller --timeout=120s
fi

TUNNEL_ACTIVE=false
if [ -f "${TUNNEL_PID_FILE}" ] && kill -0 "$(cat "${TUNNEL_PID_FILE}")" 2>/dev/null; then
  echo "minikube tunnel is already running (pid $(cat "${TUNNEL_PID_FILE}")) — skipping port-forward, portless access is active"
  TUNNEL_ACTIVE=true
elif [ -f "${PORT_FORWARD_PID_FILE}" ] && kill -0 "$(cat "${PORT_FORWARD_PID_FILE}")" 2>/dev/null; then
  echo "Port-forward already running (pid $(cat "${PORT_FORWARD_PID_FILE}"))"
else
  echo "Starting background port-forward: localhost:${LOCAL_HTTP_PORT}/${LOCAL_HTTPS_PORT} -> ingress-nginx-controller..."
  nohup kubectl --context "${MINIKUBE_PROFILE}" port-forward -n ingress-nginx svc/ingress-nginx-controller \
    "${LOCAL_HTTP_PORT}:80" "${LOCAL_HTTPS_PORT}:443" \
    > "${PORT_FORWARD_LOG_FILE}" 2>&1 &
  echo $! > "${PORT_FORWARD_PID_FILE}"
  sleep 3
  kill -0 "$(cat "${PORT_FORWARD_PID_FILE}")" 2>/dev/null \
    || { echo "port-forward failed to start, check ${PORT_FORWARD_LOG_FILE}" >&2; exit 1; }
fi

cat > "${CLUSTER_ENV_FILE}" <<EOF
export CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}
export KUBECONTEXT=${MINIKUBE_PROFILE}
export LOCAL_HTTP_PORT=${LOCAL_HTTP_PORT}
export LOCAL_HTTPS_PORT=${LOCAL_HTTPS_PORT}
EOF

if [ "${TUNNEL_ACTIVE}" = true ]; then
  SMOKE_HTTP_PORT=80
  SMOKE_HTTPS_PORT=443
else
  SMOKE_HTTP_PORT="${LOCAL_HTTP_PORT}"
  SMOKE_HTTPS_PORT="${LOCAL_HTTPS_PORT}"
fi

# --- Verify the exposure path actually works end to end --------------------
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

# The SSL-passthrough patch above replaces the ingress-nginx-controller pod.
# `kubectl rollout status` only confirms the new pod passed its /healthz
# readiness probe — it says nothing about kube-proxy having programmed the
# new pod's IP into the ingress-nginx-controller-admission Service's
# dataplane yet. Applying this Ingress (which goes through that validating
# webhook) can land in that gap and get "connection refused". Retry instead
# of guessing a fixed delay — the apply is idempotent.
for attempt in $(seq 1 15); do
  if kubectl apply -f "${SMOKE_MANIFEST}"; then
    break
  fi
  [ "${attempt}" -eq 15 ] && { echo "Failed to apply smoke test manifest after retries" >&2; exit 1; }
  sleep 2
done
kubectl wait --for=condition=ready pod -l app=cluster-setup-smoke-test --timeout=180s -n "${SMOKE_NAMESPACE}"

echo "Checking http://${SMOKE_HOST}:${SMOKE_HTTP_PORT}/ (ingress-nginx reload can take up to ~30s)..."
wait_for_http "http://${SMOKE_HOST}:${SMOKE_HTTP_PORT}/" ""
echo "Checking https://${SMOKE_HOST}:${SMOKE_HTTPS_PORT}/ ..."
wait_for_http "https://${SMOKE_HOST}:${SMOKE_HTTPS_PORT}/" "-k"
if [ "${TUNNEL_ACTIVE}" = true ]; then
  echo "Smoke test passed: macOS -> sudo minikube tunnel -> ingress-nginx -> pod is working (minikube)."
else
  echo "Smoke test passed: macOS -> kubectl port-forward -> ingress-nginx -> pod is working (minikube)."
fi

cleanup_smoke_test
trap - EXIT

echo ""
echo "Cluster ready. To use it in this shell:"
echo "  source ${CLUSTER_ENV_FILE}"
echo ""
echo "CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}"
echo "kubectl context: ${MINIKUBE_PROFILE}"
if [ "${TUNNEL_ACTIVE}" = true ]; then
  echo "Access via https://<name>.${CONSOLE_CLUSTER_DOMAIN}/ (portless, via minikube tunnel)"
else
  echo "Access via https://<name>.${CONSOLE_CLUSTER_DOMAIN}:${LOCAL_HTTPS_PORT}/ (note the :${LOCAL_HTTPS_PORT} — run enable-tunnel.sh for portless access)"
fi
if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  echo "podman machine sizing: PODMAN_MACHINE_CPUS=${PODMAN_MACHINE_CPUS} PODMAN_MACHINE_MEMORY=${PODMAN_MACHINE_MEMORY}MB (only applied on first-time machine init)"
fi
