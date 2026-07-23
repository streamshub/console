#!/usr/bin/env bash
# Switches ingress-nginx exposure from create-cluster.sh's default
# (kubectl port-forward on high ports, no sudo needed) to portless access
# via `minikube tunnel` — matching kind's UX (no :8443 suffix in URLs,
# needed e.g. for OIDC/auth redirect flows that assume no port).
#
# `minikube tunnel` only manages Services of type LoadBalancer (not
# NodePort, which is what the ingress addon creates by default), and
# binding privileged ports 80/443 requires sudo. This script handles that
# itself via `sudo -v` — you'll be prompted for your password once, right
# here, then the rest runs unattended. Run this script directly (don't
# prefix the whole thing with `sudo` yourself — only the one command that
# actually needs it is elevated).
#
# Usage: ./enable-tunnel.sh
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib/env.sh

if [ -f "${PORT_FORWARD_PID_FILE}" ]; then
  pid=$(cat "${PORT_FORWARD_PID_FILE}")
  kill "${pid}" 2>/dev/null || true
  rm -f "${PORT_FORWARD_PID_FILE}" "${PORT_FORWARD_LOG_FILE}"
  echo "Stopped the kubectl port-forward (pid ${pid}) — the tunnel replaces it."
fi

if [ -f "${TUNNEL_PID_FILE}" ] && kill -0 "$(cat "${TUNNEL_PID_FILE}")" 2>/dev/null; then
  echo "Tunnel already running (pid $(cat "${TUNNEL_PID_FILE}")), skipping start"
else
  echo "Patching ingress-nginx-controller Service to type LoadBalancer..."
  kubectl -n ingress-nginx patch svc ingress-nginx-controller -p '{"spec":{"type":"LoadBalancer"}}'

  echo ""
  echo "minikube tunnel needs to bind privileged ports 80/443, which requires sudo."
  echo "You'll be prompted for your password now (once) — everything after that runs unattended."
  sudo -v

  echo "Starting background tunnel: sudo minikube tunnel -p ${MINIKUBE_PROFILE}..."
  nohup sudo minikube tunnel -p "${MINIKUBE_PROFILE}" > "${TUNNEL_LOG_FILE}" 2>&1 &
  echo $! > "${TUNNEL_PID_FILE}"
  sleep 3
  kill -0 "$(cat "${TUNNEL_PID_FILE}")" 2>/dev/null \
    || { echo "tunnel failed to start, check ${TUNNEL_LOG_FILE}" >&2; exit 1; }
fi

cat > "${CLUSTER_ENV_FILE}" <<EOF
export CONSOLE_CLUSTER_DOMAIN=${CONSOLE_CLUSTER_DOMAIN}
export KUBECONTEXT=${MINIKUBE_PROFILE}
EOF

echo ""
echo "Waiting for the tunnel to actually bind (EXTERNAL-IP -> 127.0.0.1)..."
ext_ip=""
for i in $(seq 1 20); do
  ext_ip=$(kubectl -n ingress-nginx get svc ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)
  [ "${ext_ip}" = "127.0.0.1" ] && break
  sleep 2
done

if [ "${ext_ip}" = "127.0.0.1" ]; then
  echo ""
  echo "Portless access ready: https://<name>.${CONSOLE_CLUSTER_DOMAIN}/"
else
  echo ""
  echo "Tunnel process started but EXTERNAL-IP isn't 127.0.0.1 yet — check ${TUNNEL_LOG_FILE}" >&2
  echo "(kubectl -n ingress-nginx get svc ingress-nginx-controller to check current status)" >&2
fi
