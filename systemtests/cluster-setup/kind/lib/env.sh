#!/usr/bin/env bash
# Shared config for kind/*.sh. Source, don't execute.

CONTAINER_ENGINE="${CONTAINER_ENGINE:-podman}"
CLUSTER_NAME="${CLUSTER_NAME:-console-local}"
INGRESS_HTTP_PORT="${INGRESS_HTTP_PORT:-80}"
INGRESS_HTTPS_PORT="${INGRESS_HTTPS_PORT:-443}"
CONSOLE_CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN:-127.0.0.1.nip.io}"

case "${CONTAINER_ENGINE}" in
  podman) export KIND_EXPERIMENTAL_PROVIDER=podman ;;
  docker) unset KIND_EXPERIMENTAL_PROVIDER ;;
  *)
    echo "Unsupported CONTAINER_ENGINE=${CONTAINER_ENGINE} (expected 'podman' or 'docker')" >&2
    exit 1
    ;;
esac

KIND_CONTEXT="kind-${CLUSTER_NAME}"
CLUSTER_ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.cluster-env"

# Podman machine sizing (only relevant when CONTAINER_ENGINE=podman). Defaults
# to (host CPUs - 1) and (host memory - 4GB headroom) so macOS itself isn't
# starved; override with PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY (MB).
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

# Ensures a podman machine exists and is running. Only sizes it (via
# PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY) on first-time init — an existing
# machine is never resized automatically, since that requires stopping it
# (killing anything running inside, including the kind cluster).
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
