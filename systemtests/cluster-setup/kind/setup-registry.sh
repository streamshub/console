#!/usr/bin/env bash
# Sets up a local image registry for the kind cluster, following kind's own
# documented pattern (https://kind.sigs.k8s.io/docs/user/local-registry/): a
# registry container on the kind network, with each node's containerd
# configured to resolve it.
#
# Needed because OLM's CatalogSource controller hardcodes
# `imagePullPolicy: Always` on the registry pod it creates for a
# CatalogSource — kubelet always does a real pull for the catalog image, so
# loading it straight into containerd isn't enough; it has to actually be
# resolvable and pullable from inside the cluster, same as any other image.
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

REGISTRY_NAME="${CLUSTER_NAME}-registry"

if "${CONTAINER_ENGINE}" inspect "${REGISTRY_NAME}" >/dev/null 2>&1; then
  echo "Registry container '${REGISTRY_NAME}' already exists, skipping create"
else
  echo "Starting local registry container '${REGISTRY_NAME}' on 127.0.0.1:${REGISTRY_PORT}..."
  "${CONTAINER_ENGINE}" run -d --restart=always \
    -p "127.0.0.1:${REGISTRY_PORT}:5000" \
    --name "${REGISTRY_NAME}" \
    registry:2
fi

# Connect the registry to kind's own network so nodes can resolve it by
# name. Idempotent: connecting an already-connected container just errors,
# which is fine to ignore.
"${CONTAINER_ENGINE}" network connect kind "${REGISTRY_NAME}" 2>/dev/null || true

# Point each node's containerd at the registry container whenever something
# asks to pull "localhost:${REGISTRY_PORT}/...". kindest/node images already
# have containerd's config_path pointed at /etc/containerd/certs.d, so
# dropping a hosts.toml here is all that's needed (no cluster restart).
for node in $(kind get nodes --name "${CLUSTER_NAME}"); do
  echo "Configuring containerd on node '${node}' to resolve localhost:${REGISTRY_PORT} -> ${REGISTRY_NAME}:5000..."
  "${CONTAINER_ENGINE}" exec "${node}" mkdir -p "/etc/containerd/certs.d/localhost:${REGISTRY_PORT}"
  printf '[host."http://%s:5000"]\n' "${REGISTRY_NAME}" \
    | "${CONTAINER_ENGINE}" exec -i "${node}" cp /dev/stdin "/etc/containerd/certs.d/localhost:${REGISTRY_PORT}/hosts.toml"
done

echo ""
echo "Registry ready. Push to localhost:${REGISTRY_PORT}/... from this host; the cluster"
echo "resolves and pulls the same name back from the registry container."
