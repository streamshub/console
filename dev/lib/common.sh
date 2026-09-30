#!/usr/bin/env bash
# Shared configuration and helpers for the dev/ toolkit. Source, don't execute.
#
# Every tunable below can be overridden from the environment, e.g.
#   CONTAINER_ENGINE=podman CONSOLE_CLUSTER_DOMAIN=127.0.0.1.nip.io ./dev.sh up

# --- Paths ------------------------------------------------------------------
# LIB_DIR = dev/lib, DEV_DIR = dev, REPO_ROOT = repository root.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEV_DIR="$(cd "${LIB_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${DEV_DIR}/.." && pwd)"
GEN_DIR="${DEV_DIR}/.gen"
MANIFESTS_DIR="${DEV_DIR}/manifests"
CLUSTER_SETUP_DIR="${REPO_ROOT}/systemtests/cluster-setup"

# --- Cluster / topology defaults --------------------------------------------
export CLUSTER_NAME="${CLUSTER_NAME:-console-local}"
export CONSOLE_CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN:-127.0.0.1.nip.io}"
export INGRESS_HTTP_PORT="${INGRESS_HTTP_PORT:-80}"
export INGRESS_HTTPS_PORT="${INGRESS_HTTPS_PORT:-443}"
KUBECONTEXT="kind-${CLUSTER_NAME}"

KAFKA_NAMESPACE="${KAFKA_NAMESPACE:-kafka}"
STRIMZI_NAMESPACE="${STRIMZI_NAMESPACE:-strimzi}"
KAFKA_NAME="console-kafka"
KAFKA_USER="console-kafka-user1"

# --- Versions ---------------------------------------------------------------
STRIMZI_VERSION="${STRIMZI_VERSION:-0.51.0}"
PROMETHEUS_OPERATOR_VERSION="${PROMETHEUS_OPERATOR_VERSION:-v0.92.1}"
export APICURIO_IMAGE="${APICURIO_IMAGE:-quay.io/apicurio/apicurio-registry-mem:2.6.13.Final}"
export KEYCLOAK_IMAGE="${KEYCLOAK_IMAGE:-quay.io/keycloak/keycloak:26.7}"
# Operator mode deploys a *released* console-api image (the current SNAPSHOT
# isn't published). Override with CONSOLE_API_IMAGE to test a specific build.
CONSOLE_API_IMAGE="${CONSOLE_API_IMAGE:-quay.io/streamshub/console-api:0.14.1}"

# --- Host OS ----------------------------------------------------------------
OS_NAME="$(uname -s)"   # Darwin | Linux

# --- Container engine -------------------------------------------------------
# Default to docker (Colima on macOS, the native daemon on Linux) — the repo's
# verified-reliable path for the full Kafka+Console workload. podman is fully
# supported as an alternative.
if [ -z "${CONTAINER_ENGINE:-}" ]; then
  if command -v docker >/dev/null 2>&1; then
    CONTAINER_ENGINE="docker"
  elif command -v podman >/dev/null 2>&1; then
    CONTAINER_ENGINE="podman"
  else
    CONTAINER_ENGINE="docker"
  fi
fi
export CONTAINER_ENGINE

# --- Logging ----------------------------------------------------------------
if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_BOLD=$'\033[1m'; C_BLUE=$'\033[34m'
  C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'
else
  C_RESET=""; C_BOLD=""; C_BLUE=""; C_GREEN=""; C_YELLOW=""; C_RED=""
fi
info() { echo "${C_BLUE}${C_BOLD}==>${C_RESET} $*"; }
step() { echo "${C_GREEN}${C_BOLD} -${C_RESET} $*"; }
warn() { echo "${C_YELLOW}${C_BOLD}warning:${C_RESET} $*" >&2; }
err()  { echo "${C_RED}${C_BOLD}error:${C_RESET} $*" >&2; }
die()  { err "$@"; exit 1; }

# --- Prerequisite checks ----------------------------------------------------
require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' not found in PATH. $2"
}

check_prereqs() {
  require_cmd kind   "Install with: brew install kind"
  require_cmd kubectl "Install with: brew install kubectl"
  require_cmd helm   "Install with: brew install helm"
  require_cmd jq     "Install with: brew install jq"
  require_cmd envsubst "Install with: brew install gettext (and 'brew link --force gettext')"

  case "${CONTAINER_ENGINE}" in
    docker)
      if ! command -v docker >/dev/null 2>&1; then
        if [ "${OS_NAME}" = "Darwin" ]; then
          die "CONTAINER_ENGINE=docker but the 'docker' CLI isn't installed.
  On macOS with Colima you still need the docker client: brew install docker
  Then start the VM: colima start --cpus 6 --memory 16 --disk 60
  Or switch engines: CONTAINER_ENGINE=podman ./dev.sh ..."
        else
          die "CONTAINER_ENGINE=docker but the 'docker' CLI isn't installed.
  Install Docker Engine for your distro (https://docs.docker.com/engine/install/),
  or switch engines: CONTAINER_ENGINE=podman ./dev.sh ..."
        fi
      fi
      if ! docker info >/dev/null 2>&1; then
        if [ "${OS_NAME}" = "Darwin" ]; then
          die "The docker daemon isn't reachable.
  If you use Colima: colima start --cpus 6 --memory 16 --disk 60
  See dev/README.md (Container engines) for sizing guidance."
        else
          die "The docker daemon isn't reachable.
  Start it (sudo systemctl start docker) and ensure your user can use it
  (add yourself to the 'docker' group, or run rootless docker).
  See dev/README.md (Container engines / On Linux) for details."
        fi
      fi
      ;;
    podman)
      require_cmd podman "Install with: brew install podman (macOS) or your distro's package manager"
      ;;
    *)
      die "Unsupported CONTAINER_ENGINE=${CONTAINER_ENGINE} (expected 'docker' or 'podman')"
      ;;
  esac
}

# Guard: refuse to operate unless the active kube context is our kind cluster,
# so we never touch a real cluster by accident.
ensure_context() {
  kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}" \
    || die "kind cluster '${CLUSTER_NAME}' doesn't exist yet. Run: ${DEV_DIR}/dev.sh up"
  kubectl config use-context "${KUBECONTEXT}" >/dev/null \
    || die "kube context '${KUBECONTEXT}' not found"
}

# Variables our manifest templates use. Restricting envsubst to this explicit
# set is essential: manifests also contain literal `${1}` / `$1` tokens (JMX
# exporter rules, Prometheus relabel replacements) that a blanket envsubst
# would wrongly expand to empty. envsubst only touches names listed here.
TEMPLATE_VARS='${CLUSTER_DOMAIN} ${NAMESPACE} ${KAFKA_NAMESPACE} ${APICURIO_IMAGE} ${KEYCLOAK_IMAGE} ${LISTENER_TYPE}'

# Apply a manifest file through envsubst (restricted to TEMPLATE_VARS).
apply_templated() {
  local file="$1" ns="${2:-}"
  export CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN}"
  export NAMESPACE="${ns}"
  export KAFKA_NAMESPACE
  if [ -n "${ns}" ]; then
    kubectl create namespace "${ns}" --dry-run=client -o yaml | kubectl apply -f - >/dev/null
    envsubst "${TEMPLATE_VARS}" < "${file}" | kubectl apply -n "${ns}" -f -
  else
    envsubst "${TEMPLATE_VARS}" < "${file}" | kubectl apply -f -
  fi
}

mkgen() { mkdir -p "${GEN_DIR}"; }
