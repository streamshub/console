#!/usr/bin/env bash
# Shared config for minikube/*.sh. Source, don't execute.

OS_NAME="$(uname -s)"

# Auto-detect the container engine if not explicitly set: prefer podman,
# fall back to docker, so this works out of the box on machines that only
# have one of the two installed (common on Linux, where docker is often
# the default with no podman in sight).
if [ -z "${CONTAINER_ENGINE:-}" ]; then
  if command -v podman >/dev/null 2>&1; then
    CONTAINER_ENGINE="podman"
  elif command -v docker >/dev/null 2>&1; then
    CONTAINER_ENGINE="docker"
  else
    echo "Neither podman nor docker found in PATH. Install one, or set CONTAINER_ENGINE explicitly." >&2
    exit 1
  fi
fi

CLUSTER_NAME="${CLUSTER_NAME:-console-minikube}"
CONSOLE_CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN:-127.0.0.1.nip.io}"

# minikube's ingress addon exposes a NodePort Service. On macOS, direct
# NodePort access to the minikube node IP times out (Docker/Podman Desktop
# run the engine inside a VM), so we expose it via a persistent
# `kubectl port-forward` on these (non-privileged, no sudo needed) local
# ports instead. On native Linux, the node IP is normally directly
# reachable and this port-forward isn't strictly necessary — but it works
# there too, so it's used uniformly on both OSes rather than branching.
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

# Podman machine sizing (only relevant on macOS, where podman needs a VM to
# run containers at all — native Linux podman doesn't). Same logic as
# ../kind/lib/env.sh, duplicated rather than sourced to keep this folder
# fully independent. Defaults to (host CPUs - 1) and (host memory - 4GB
# headroom); override with PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY (MB).
# Portable: prefers Linux-native tools (nproc, /proc/meminfo), falls back
# to macOS's sysctl.
detect_default_cpus() {
  local total
  if command -v nproc >/dev/null 2>&1; then
    total=$(nproc)
  else
    total=$(sysctl -n hw.ncpu)
  fi
  if [ "${total}" -gt 1 ]; then
    echo $(( total - 1 ))
  else
    echo "${total}"
  fi
}

detect_default_memory_mb() {
  local total_mb headroom_mb=4096
  if [ -r /proc/meminfo ]; then
    total_mb=$(( $(awk '/^MemTotal:/ {print $2}' /proc/meminfo) / 1024 ))
  else
    total_mb=$(( $(sysctl -n hw.memsize) / 1024 / 1024 ))
  fi
  if [ "${total_mb}" -gt "${headroom_mb}" ]; then
    echo $(( total_mb - headroom_mb ))
  else
    echo "${total_mb}"
  fi
}

PODMAN_MACHINE_CPUS="${PODMAN_MACHINE_CPUS:-$(detect_default_cpus)}"
PODMAN_MACHINE_MEMORY="${PODMAN_MACHINE_MEMORY:-$(detect_default_memory_mb)}"

# Ensures a podman machine exists and is running — but only on macOS.
# Native Linux podman talks to the local system directly; there's no VM to
# create or manage, so this is a no-op there regardless of CONTAINER_ENGINE.
ensure_podman_machine() {
  if [ "${OS_NAME}" != "Darwin" ]; then
    return 0
  fi

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
