#!/usr/bin/env bash
# Sets up a local image registry for the minikube cluster, using minikube's
# own `registry` addon. The addon's registry-proxy DaemonSet binds
# hostNetwork port 5000 on every node, so "localhost:5000" already resolves
# correctly *from inside the cluster* with zero extra config. What's
# missing is reaching it *from the host* to push into — exposed here via a
# persistent `kubectl port-forward`, the same non-privileged, no-sudo
# approach create-cluster.sh already uses for ingress (direct node-IP
# access doesn't reliably work on macOS; see ../README.md).
#
# Needed because OLM's CatalogSource controller hardcodes
# `imagePullPolicy: Always` on the registry pod it creates — kubelet always
# does a real pull for the catalog image, so loading it straight into the
# node isn't enough; it has to actually be resolvable and pullable from
# inside the cluster, same as any other image.
#
# Usage:
#   ./setup-registry.sh [--registry-port 5000]
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./env.sh

REGISTRY_PORT="${IMAGE_REGISTRY_PORT:-5000}"

usage() {
  echo "Usage: $0 [--registry-port 5000]" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --registry-port) REGISTRY_PORT="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown argument: $1" >&2; usage ;;
  esac
done

echo "Enabling registry addon on profile '${MINIKUBE_PROFILE}' (no-op if already enabled)..."
minikube -p "${MINIKUBE_PROFILE}" addons enable registry

kubectl --context "${MINIKUBE_PROFILE}" -n kube-system wait --for=condition=ready pod \
  --selector=kubernetes.io/minikube-addons=registry --timeout=180s

if [ -f "${REGISTRY_PORT_FORWARD_PID_FILE}" ] && kill -0 "$(cat "${REGISTRY_PORT_FORWARD_PID_FILE}")" 2>/dev/null; then
  echo "Registry port-forward already running (pid $(cat "${REGISTRY_PORT_FORWARD_PID_FILE}"))"
else
  echo "Starting background port-forward: localhost:${REGISTRY_PORT} -> registry addon..."
  nohup kubectl --context "${MINIKUBE_PROFILE}" port-forward -n kube-system svc/registry \
    "${REGISTRY_PORT}:80" \
    > "${REGISTRY_PORT_FORWARD_LOG_FILE}" 2>&1 &
  echo $! > "${REGISTRY_PORT_FORWARD_PID_FILE}"
  sleep 3
  kill -0 "$(cat "${REGISTRY_PORT_FORWARD_PID_FILE}")" 2>/dev/null \
    || { echo "Registry port-forward failed to start, check ${REGISTRY_PORT_FORWARD_LOG_FILE}" >&2; exit 1; }
fi

echo ""
echo "Registry ready. Push to localhost:${REGISTRY_PORT}/... from this host; the cluster"
echo "resolves and pulls the same name back via the registry addon."
