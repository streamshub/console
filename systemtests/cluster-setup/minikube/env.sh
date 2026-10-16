#!/usr/bin/env bash
# Shared config for minikube/*.sh. Source, don't execute.

source "$(dirname "${BASH_SOURCE[0]}")/../common/common.sh"

# Installs minikube from the official release binary for the detected
# OS/arch. Used by ensure_installed below so every minikube/*.sh script
# self-installs minikube on first use, matching what CI used to do as a
# separate "Install minikube" step.
install_minikube() {
  local os arch url

  case "${OS_NAME}" in
    Darwin) os=darwin ;;
    Linux)  os=linux ;;
  esac

  case "$(uname -m)" in
    arm64|aarch64) arch=arm64 ;;
    x86_64|amd64)  arch=amd64 ;;
    *)
      echo "Unsupported architecture for minikube auto-install: $(uname -m)." >&2
      echo "Install minikube manually: https://minikube.sigs.k8s.io/docs/start/" >&2
      exit 1
      ;;
  esac

  url="https://storage.googleapis.com/minikube/releases/latest/minikube-${os}-${arch}"
  echo "Downloading minikube from ${url}..."
  curl -Lo /tmp/minikube "${url}"
  chmod +x /tmp/minikube
  sudo mv /tmp/minikube /usr/local/bin/minikube
}

ensure_installed minikube install_minikube

# Auto-detect the minikube driver if not explicitly set. Minikube's own
# driver docs (https://minikube.sigs.k8s.io/docs/drivers/) list docker as
# preferred on both platforms; podman is still marked experimental on
# both. Prefer docker, then the platform's VM-based driver (vfkit on
# macOS, kvm2 on Linux), with podman only as a last resort — confirmed
# painful in practice: the ip_tables kernel module requirement, PID-limit
# crashes under a full Kafka+Console workload, and a rootless-podman
# failure hit directly against minikube.
if [ -z "${CONTAINER_ENGINE:-}" ]; then
  if command -v docker >/dev/null 2>&1; then
    CONTAINER_ENGINE="docker"
  elif [ "${OS_NAME}" = "Darwin" ] && command -v vfkit >/dev/null 2>&1; then
    CONTAINER_ENGINE="vfkit"
  elif [ "${OS_NAME}" = "Linux" ] && command -v virsh >/dev/null 2>&1; then
    CONTAINER_ENGINE="kvm2"
  elif command -v podman >/dev/null 2>&1; then
    CONTAINER_ENGINE="podman"
  else
    echo "No supported minikube driver found (docker, vfkit/kvm2, or podman). Install one, or set CONTAINER_ENGINE explicitly." >&2
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
  docker|podman|vfkit|kvm2) ;;
  *)
    echo "Unsupported CONTAINER_ENGINE=${CONTAINER_ENGINE} (expected 'docker', 'podman', 'vfkit', or 'kvm2')" >&2
    exit 1
    ;;
esac

MINIKUBE_PROFILE="${CLUSTER_NAME}"
CLUSTER_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLUSTER_ENV_FILE="${CLUSTER_ROOT}/.cluster-env"
PORT_FORWARD_PID_FILE="${CLUSTER_ROOT}/.port-forward.pid"
PORT_FORWARD_LOG_FILE="${CLUSTER_ROOT}/.port-forward.log"
TUNNEL_PID_FILE="${CLUSTER_ROOT}/.tunnel.pid"
TUNNEL_LOG_FILE="${CLUSTER_ROOT}/.tunnel.log"
REGISTRY_PORT_FORWARD_PID_FILE="${CLUSTER_ROOT}/.registry-port-forward.pid"
REGISTRY_PORT_FORWARD_LOG_FILE="${CLUSTER_ROOT}/.registry-port-forward.log"
