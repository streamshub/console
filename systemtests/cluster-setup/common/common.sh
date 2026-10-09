#!/usr/bin/env bash
# Shared utilities for cluster-setup/{kind,minikube}. Source, don't execute.

OS_NAME="$(uname -s)"

# The only OS-conditional behavior left after unifying with
# systemtests/scripts/setup-minikube.sh is local exposure (reaching the
# cluster's ingress/registry from the host): Linux can reach the
# minikube/kind node directly; macOS's container engines run inside a
# hidden VM and can't. NEEDS_LOCAL_PORT_FORWARD is that one switch.
case "${OS_NAME}" in
  Darwin) NEEDS_LOCAL_PORT_FORWARD=true ;;
  Linux)  NEEDS_LOCAL_PORT_FORWARD=false ;;
  *)
    echo "Unsupported OS_NAME=${OS_NAME} (expected 'Darwin' or 'Linux')" >&2
    exit 1
    ;;
esac

# Podman machine sizing (only relevant on macOS, where podman needs a VM to
# run containers at all — native Linux podman doesn't). Defaults to (host
# CPUs - 1) and (host memory - 4GB headroom) so the host itself isn't
# starved; override with PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY (MB).
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
# Only sizes the machine (via PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY) on
# first-time init — an existing machine is never resized automatically,
# since that requires stopping it (killing anything running inside,
# including the kind cluster).
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

# On native Linux with rootless podman, kube-proxy/ingress-nginx need the
# host's ip_tables kernel module loaded — rootless containers can't load
# kernel modules themselves (no CAP_SYS_MODULE), so a missing module
# surfaces confusingly later as ingress-nginx/kube-proxy failing, not as an
# obvious "module missing" error. Check up front and fail fast instead.
# No-op on macOS (podman machine VM, handled by ensure_podman_machine) and
# on rootful podman/docker (which don't hit this).
check_linux_rootless_podman_ip_tables() {
  if [ "${OS_NAME}" != "Linux" ] || [ "${CONTAINER_ENGINE}" != "podman" ]; then
    return 0
  fi

  if [ "$(podman info --format json | jq -r .host.security.rootless)" != "true" ]; then
    return 0
  fi

  if [ -z "$(lsmod | grep ^ip_tables)" ]; then
    echo "podman is running rootless on Linux, but the 'ip_tables' kernel module isn't loaded." >&2
    echo "Rootless containers can't load kernel modules themselves (no CAP_SYS_MODULE), and" >&2
    echo "kube-proxy/ingress-nginx need it on the host - without it, ingress setup fails." >&2
    echo "" >&2
    echo "Fix:" >&2
    echo "  sudo modprobe ip_tables" >&2
    echo "  echo ip_tables | sudo tee /etc/modules-load.d/ip_tables.conf   # persist across reboots" >&2
    exit 1
  fi
}
