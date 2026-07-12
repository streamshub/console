#!/usr/bin/env bash
# Shared config for minikube/*.sh. Source, don't execute.

CONTAINER_ENGINE="${CONTAINER_ENGINE:-podman}"
CLUSTER_NAME="${CLUSTER_NAME:-console-minikube}"
CONSOLE_CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN:-127.0.0.1.nip.io}"

# minikube's ingress addon exposes a NodePort Service, and direct NodePort
# access to the minikube node IP times out from macOS (confirmed on this
# machine, matches long-standing upstream minikube issues on docker/podman
# drivers). We expose it instead via a persistent `kubectl port-forward` on
# these (non-privileged, no sudo needed) local ports. See
# ~/claude-docs/streamshub-local-sts/README.md for the full investigation,
# including a portless (sudo-tunnel) alternative if you need it (e.g. for
# OIDC/auth flows that assume no port in the redirect URL).
LOCAL_HTTP_PORT="${LOCAL_HTTP_PORT:-8080}"
LOCAL_HTTPS_PORT="${LOCAL_HTTPS_PORT:-8443}"

case "${CONTAINER_ENGINE}" in
  podman|docker) ;;
  *)
    echo "Unsupported CONTAINER_ENGINE=${CONTAINER_ENGINE} (expected 'podman' or 'docker')" >&2
    exit 1
    ;;
esac

MINIKUBE_PROFILE="${CLUSTER_NAME}"
CLUSTER_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER_ENV_FILE="${CLUSTER_ROOT}/.cluster-env"
PORT_FORWARD_PID_FILE="${CLUSTER_ROOT}/.port-forward.pid"
PORT_FORWARD_LOG_FILE="${CLUSTER_ROOT}/.port-forward.log"
TUNNEL_PID_FILE="${CLUSTER_ROOT}/.tunnel.pid"
TUNNEL_LOG_FILE="${CLUSTER_ROOT}/.tunnel.log"

# Podman machine sizing (only relevant when CONTAINER_ENGINE=podman). Same
# logic as ../kind/lib/env.sh, duplicated rather than sourced to keep this
# folder fully independent (per instructions: don't touch/depend on the
# kind setup's internals). Defaults to (host CPUs - 1) and (host memory
# - 4GB headroom); override with PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY
# (MB).
detect_default_cpus() {
  local total
  total=$(sysctl -n hw.ncpu)
  if [ "${total}" -gt 1 ]; then
    echo $(( total - 1 ))
  else
    echo "${total}"
  fi
}

detect_default_memory_mb() {
  local total_mb headroom_mb=4096
  total_mb=$(( $(sysctl -n hw.memsize) / 1024 / 1024 ))
  if [ "${total_mb}" -gt "${headroom_mb}" ]; then
    echo $(( total_mb - headroom_mb ))
  else
    echo "${total_mb}"
  fi
}

PODMAN_MACHINE_CPUS="${PODMAN_MACHINE_CPUS:-$(detect_default_cpus)}"
PODMAN_MACHINE_MEMORY="${PODMAN_MACHINE_MEMORY:-$(detect_default_memory_mb)}"

ensure_podman_machine() {
  local name state cpus mem

  name=$(podman machine list --format json | jq -r '.[0].Name // empty')

  if [ -z "${name}" ]; then
    echo "No podman machine found — initializing 'podman-machine-default' with ${PODMAN_MACHINE_CPUS} CPUs / ${PODMAN_MACHINE_MEMORY}MB memory..."
    podman machine init --rootful --cpus "${PODMAN_MACHINE_CPUS}" --memory "${PODMAN_MACHINE_MEMORY}"
    podman machine start
    return
  fi

  state=$(podman machine inspect "${name}" | jq -r '.[0].State')
  if [ "${state}" != "running" ]; then
    echo "Starting existing podman machine '${name}'..."
    podman machine start "${name}"
  fi

  cpus=$(podman machine inspect "${name}" | jq -r '.[0].Resources.CPUs')
  mem=$(podman machine inspect "${name}" | jq -r '.[0].Resources.Memory')
  if [ "${cpus}" != "${PODMAN_MACHINE_CPUS}" ] || [ "${mem}" != "${PODMAN_MACHINE_MEMORY}" ]; then
    echo "Note: podman machine '${name}' is already sized at ${cpus} CPUs / ${mem}MB memory (requested ${PODMAN_MACHINE_CPUS}/${PODMAN_MACHINE_MEMORY}MB). Not resizing an existing machine automatically." >&2
    echo "  To resize: podman machine stop ${name} && podman machine set --cpus ${PODMAN_MACHINE_CPUS} --memory ${PODMAN_MACHINE_MEMORY} ${name} && podman machine start ${name}" >&2
  fi
}
