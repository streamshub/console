#!/usr/bin/env bash
# Shared config for kind/*.sh. Source, don't execute.

source "$(dirname "${BASH_SOURCE[0]}")/../common/common.sh"

# Auto-detect the container engine if not explicitly set: prefer docker,
# fall back to podman. kind only ever supports these two, and docker has
# been the more reliable of the two in practice (podman has repeatedly
# needed workarounds: the ip_tables kernel module requirement on Linux,
# and PID-limit crashes under a full Kafka+Console workload).
if [ -z "${CONTAINER_ENGINE:-}" ]; then
  if command -v docker >/dev/null 2>&1; then
    CONTAINER_ENGINE="docker"
  elif command -v podman >/dev/null 2>&1; then
    CONTAINER_ENGINE="podman"
  else
    echo "Neither docker nor podman found in PATH. Install one, or set CONTAINER_ENGINE explicitly." >&2
    exit 1
  fi
fi

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
CLUSTER_ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.cluster-env"
