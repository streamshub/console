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
source ./env.sh

check_linux_rootless_podman_ip_tables

if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  ensure_podman_machine
fi

MINIKUBE_CPU_COUNT="${MINIKUBE_CPU_COUNT:-}"
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-}"
MINIKUBE_DISK_SIZE="${MINIKUBE_DISK_SIZE:-}"
MINIKUBE_EXTRA_ADDONS="${MINIKUBE_EXTRA_ADDONS:-}"
MINIKUBE_INSECURE_REGISTRY="${MINIKUBE_INSECURE_REGISTRY:-}"
MINIKUBE_EXTRA_CONFIG="${MINIKUBE_EXTRA_CONFIG:-}"

if minikube status -p "${MINIKUBE_PROFILE}" >/dev/null 2>&1; then
  echo "minikube profile '${MINIKUBE_PROFILE}' already running, skipping create"
else
  MINIKUBE_START_ARGS=(--driver="${CONTAINER_ENGINE}" --addons="ingress${MINIKUBE_EXTRA_ADDONS:+,${MINIKUBE_EXTRA_ADDONS}}")
  [ -n "${MINIKUBE_CPU_COUNT}" ] && MINIKUBE_START_ARGS+=(--cpus="${MINIKUBE_CPU_COUNT}")
  [ -n "${MINIKUBE_MEMORY}" ] && MINIKUBE_START_ARGS+=(--memory="${MINIKUBE_MEMORY}")
  [ -n "${MINIKUBE_DISK_SIZE}" ] && MINIKUBE_START_ARGS+=(--disk-size="${MINIKUBE_DISK_SIZE}")
  [ -n "${MINIKUBE_INSECURE_REGISTRY}" ] && MINIKUBE_START_ARGS+=(--insecure-registry="${MINIKUBE_INSECURE_REGISTRY}")
  if [ -n "${MINIKUBE_EXTRA_CONFIG}" ]; then
    IFS=';' read -ra _extra_cfgs <<< "${MINIKUBE_EXTRA_CONFIG}"
    for cfg in "${_extra_cfgs[@]}"; do
      MINIKUBE_START_ARGS+=(--extra-config="${cfg}")
    done
  fi
  echo "Creating minikube cluster '${MINIKUBE_PROFILE}' (driver=${CONTAINER_ENGINE})..."
  minikube start -p "${MINIKUBE_PROFILE}" "${MINIKUBE_START_ARGS[@]}"
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
LOCAL_ACCESS=false

if [ "${NEEDS_LOCAL_PORT_FORWARD}" = false ]; then
  echo "Linux: minikube node IP is directly reachable, skipping port-forward"
  LOCAL_ACCESS=true
  CONSOLE_CLUSTER_DOMAIN="$(minikube ip -p "${MINIKUBE_PROFILE}").nip.io"
elif [ -f "${TUNNEL_PID_FILE}" ] && kill -0 "$(cat "${TUNNEL_PID_FILE}")" 2>/dev/null; then
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

# Lets anything running *inside* the cluster (Console, most notably — it
# connects to Kafka's ingress-exposed "secure" listener using this same
# CONSOLE_CLUSTER_DOMAIN) actually reach the ingress controller through it.
# Called after CONSOLE_CLUSTER_DOMAIN is finalized above (it's rewritten to
# the real node IP on Linux). See common.sh:configure_dns_split_horizon for
# the full "why" — on Linux this is a harmless no-op-equivalent (the domain
# was already reachable from Pods), but applying it unconditionally avoids
# having to detect that case specially.
configure_dns_split_horizon "${CONSOLE_CLUSTER_DOMAIN}" ingress-nginx ingress-nginx-controller

if [ "${LOCAL_ACCESS}" = true ]; then
  cat > "${CLUSTER_ENV_FILE}" <<EOF
export CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}
export KUBECONTEXT=${MINIKUBE_PROFILE}
EOF
else
  cat > "${CLUSTER_ENV_FILE}" <<EOF
export CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}
export KUBECONTEXT=${MINIKUBE_PROFILE}
export LOCAL_HTTP_PORT=${LOCAL_HTTP_PORT}
export LOCAL_HTTPS_PORT=${LOCAL_HTTPS_PORT}
EOF
fi

if [ "${LOCAL_ACCESS}" = true ] || [ "${TUNNEL_ACTIVE}" = true ]; then
  SMOKE_HTTP_PORT=80
  SMOKE_HTTPS_PORT=443
else
  SMOKE_HTTP_PORT="${LOCAL_HTTP_PORT}"
  SMOKE_HTTPS_PORT="${LOCAL_HTTPS_PORT}"
fi

# --- Verify the exposure path actually works end to end --------------------
echo ""
echo "Verifying exposure path with a smoke test..."
verify_ingress_exposure "${CONSOLE_CLUSTER_DOMAIN}" "${SMOKE_HTTP_PORT}" "${SMOKE_HTTPS_PORT}"
if [ "${LOCAL_ACCESS}" = true ]; then
  echo "Smoke test passed: minikube node IP -> ingress-nginx -> pod is working (minikube)."
elif [ "${TUNNEL_ACTIVE}" = true ]; then
  echo "Smoke test passed: sudo minikube tunnel -> ingress-nginx -> pod is working (minikube)."
else
  echo "Smoke test passed: kubectl port-forward -> ingress-nginx -> pod is working (minikube)."
fi

echo ""
echo "Cluster ready. To use it in this shell:"
echo "  source ${CLUSTER_ENV_FILE}"
echo ""
echo "CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}"
echo "kubectl context: ${MINIKUBE_PROFILE}"
if [ "${LOCAL_ACCESS}" = true ]; then
  echo "Access via https://<name>.${CONSOLE_CLUSTER_DOMAIN}/ (direct node-IP access, no port-forward needed on Linux)"
elif [ "${TUNNEL_ACTIVE}" = true ]; then
  echo "Access via https://<name>.${CONSOLE_CLUSTER_DOMAIN}/ (portless, via minikube tunnel)"
else
  echo "Access via https://<name>.${CONSOLE_CLUSTER_DOMAIN}:${LOCAL_HTTPS_PORT}/ (note the :${LOCAL_HTTPS_PORT} — run enable-tunnel.sh for portless access)"
fi
if [ "${CONTAINER_ENGINE}" = "podman" ]; then
  echo "podman machine sizing: PODMAN_MACHINE_CPUS=${PODMAN_MACHINE_CPUS} PODMAN_MACHINE_MEMORY=${PODMAN_MACHINE_MEMORY}MB (only applied on first-time machine init)"
fi
