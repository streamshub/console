#!/usr/bin/env bash
# One-stop local development environment for the streamshub/console project.
#
# Provisions a kind cluster + Strimzi + Kafka (+ optional metrics/registry/
# keycloak/connect profiles) and launches whichever of the three dev modes you
# want: frontend, backend, or operator. See dev/README.md for the full guide.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"
# shellcheck source=lib/infra.sh
source "${SCRIPT_DIR}/lib/infra.sh"
# shellcheck source=lib/config-gen.sh
source "${SCRIPT_DIR}/lib/config-gen.sh"

ALL_PROFILES="metrics registry keycloak connect"

usage() {
  cat <<EOF
${C_BOLD}console dev environment${C_RESET}

Usage: dev.sh <command> [options]

Commands:
  ${C_BOLD}up${C_RESET} [--full] [--profile <list>]   Create the cluster + infrastructure
      --full                        3-broker Kafka + Cruise Control (rebalance
                                    demos). Default is a lean single-node Kafka.
      --profile metrics,registry,keycloak,connect
      --profile all                 Comma-separated, or repeat the flag.

  ${C_BOLD}backend${C_RESET}                          Run the API+UI locally (quarkus:dev) for
                                    backend development (Java live-reload, :5005).
  ${C_BOLD}frontend${C_RESET}                         Run the API+UI locally (quarkus:dev) for
                                    frontend development (React HMR via Quinoa).
  ${C_BOLD}operator${C_RESET}                         Run the operator locally (quarkus:dev); it
                                    reconciles a Console CR into the cluster.

  ${C_BOLD}config${C_RESET}                           Regenerate .gen/console-config.yaml and
                                    .gen/console-cr.yaml from the live cluster.
  ${C_BOLD}status${C_RESET}                           Show cluster + component health.
  ${C_BOLD}urls${C_RESET}                             Print component URLs.
  ${C_BOLD}down${C_RESET} [--keep-cluster]            Tear everything down (or resources only).

Common env overrides:
  CONTAINER_ENGINE=docker|podman    (default: docker)
  CLUSTER_NAME, CONSOLE_CLUSTER_DOMAIN, STRIMZI_VERSION, CONSOLE_API_IMAGE

Examples:
  dev.sh up --profile all           # full-featured stack, lean Kafka
  dev.sh backend                    # then open http://localhost:8080
  dev.sh up --full --profile metrics
  dev.sh down --keep-cluster
EOF
}

# --- Argument parsing helpers ----------------------------------------------
parse_up_args() {
  DEV_FULL="false"
  DEV_PROFILES=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --full) DEV_FULL="true"; shift ;;
      --profile)
        [ $# -ge 2 ] || die "--profile needs a value"
        _add_profiles "$2"; shift 2 ;;
      --profile=*) _add_profiles "${1#*=}"; shift ;;
      -h|--help) usage; exit 0 ;;
      *) die "Unknown option for 'up': $1" ;;
    esac
  done
  export DEV_FULL DEV_PROFILES
}

_add_profiles() {
  local csv="$1" p
  csv="${csv//,/ }"
  for p in ${csv}; do
    if [ "${p}" = "all" ]; then
      DEV_PROFILES="${ALL_PROFILES}"
      continue
    fi
    case "${p}" in
      metrics|registry|keycloak|connect) ;;
      *) die "Unknown profile '${p}' (valid: metrics registry keycloak connect all)" ;;
    esac
    has_profile "${p}" || DEV_PROFILES="${DEV_PROFILES:+${DEV_PROFILES} }${p}"
  done
}

# --- Commands ---------------------------------------------------------------
cmd_up() {
  parse_up_args "$@"
  check_prereqs
  ensure_cluster
  install_strimzi
  deploy_kafka
  deploy_profiles
  save_state
  wait_profiles_ready
  info "Generating console configuration"
  generate_console_config
  generate_console_cr
  echo ""
  info "Environment ready."
  echo "  topology: $([ "${DEV_FULL}" = true ] && echo 'full (3 brokers)' || echo 'lean (single node)')"
  echo "  profiles: ${DEV_PROFILES:-<none>}"
  echo ""
  echo "Next:"
  echo "  ${SCRIPT_DIR}/dev.sh backend    # or frontend / operator"
  echo "  ${SCRIPT_DIR}/dev.sh urls"
}

cmd_config() {
  generate_console_config
  generate_console_cr
}

cmd_backend() {
  ensure_context
  info "Generating console-config.yaml"
  generate_console_config
  info "Starting API + UI (quarkus:dev). REST+UI: http://localhost:8080  (debug: 5005)"
  echo "  Java changes hot-reload on save; press 'q' or Ctrl-C to stop."
  cd "${REPO_ROOT}"
  exec mvn -am -pl api quarkus:dev -Dconsole.config-path="${GEN_DIR}/console-config.yaml"
}

cmd_frontend() {
  ensure_context
  info "Generating console-config.yaml"
  generate_console_config
  info "Starting API + UI (quarkus:dev). Open http://localhost:8080"
  echo "  React edits under api/src/main/webui hot-reload via Quinoa (HMR)."
  echo "  Standalone Vite / Storybook: see dev/README.md (frontend workflow)."
  cd "${REPO_ROOT}"
  exec mvn -am -pl api quarkus:dev -Dconsole.config-path="${GEN_DIR}/console-config.yaml"
}

CONSOLE_CRD_NAME="consoles.console.streamshub.github.com"

# Pre-install the Console CRD before launching the operator. The Quarkus Operator
# SDK's %dev profile applies the CRD on startup but starts its informer without
# waiting for the API server to register the new CRD's REST endpoint — on a fresh
# cluster the first LIST 404s and the operator aborts. Installing the CRD (and
# waiting for it to be Established) up front removes that race.
ensure_console_crd() {
  if kubectl get crd "${CONSOLE_CRD_NAME}" >/dev/null 2>&1; then
    step "Console CRD already present"
  else
    local crd
    crd="$(ls "${REPO_ROOT}"/operator/target/kubernetes/${CONSOLE_CRD_NAME}-v1.yml 2>/dev/null | head -1 || true)"
    if [ -n "${crd}" ]; then
      step "Installing Console CRD from $(basename "${crd}")"
      kubectl apply -f "${crd}" >/dev/null
    else
      warn "Console CRD not found and not yet generated (operator not built).
  The operator will apply it on startup; if startup fails with a 'consoles ...
  Not Found' informer error, just re-run 'dev.sh operator' — the CRD will exist
  by then. To avoid this, build the operator once first: mvn -pl operator -am install -DskipTests"
      return 0
    fi
  fi
  kubectl wait --for=condition=established --timeout=60s "crd/${CONSOLE_CRD_NAME}" >/dev/null 2>&1 || true
}

cmd_operator() {
  ensure_context
  info "Generating Console CR for operator mode"
  generate_console_cr
  info "Ensuring the Console CRD is installed"
  ensure_console_crd
  info "The operator will run locally and reconcile the Console CR into the cluster."
  echo "  console-api image: ${CONSOLE_API_IMAGE}"
  echo "  The Console CR is applied automatically once the operator registers its CRD."
  # Apply the CR as soon as the (starting) operator establishes its CRD, so the
  # user just sees it get reconciled. Runs in the background; mvn runs foreground.
  (
    for _ in $(seq 1 90); do
      if kubectl get crd "${CONSOLE_CRD_NAME}" >/dev/null 2>&1; then
        kubectl apply -n "${KAFKA_NAMESPACE}" -f "${GEN_DIR}/console-cr.yaml" >/dev/null 2>&1 && break
      fi
      sleep 2
    done
  ) &
  cd "${REPO_ROOT}"
  exec mvn -am -pl operator quarkus:dev \
    -Dconsole.deployment.default-api-image="${CONSOLE_API_IMAGE}"
}

cmd_status() {
  ensure_context
  load_state
  info "Cluster '${CLUSTER_NAME}' (engine=${CONTAINER_ENGINE})"
  echo "  topology: $([ "${DEV_FULL}" = true ] && echo 'full' || echo 'lean'), profiles: ${DEV_PROFILES:-<none>}"
  echo ""
  info "Kafka"
  kubectl -n "${KAFKA_NAMESPACE}" get kafka,kafkanodepool,kafkaconnect 2>/dev/null || true
  echo ""
  info "Pods"
  for ns in "${STRIMZI_NAMESPACE}" "${KAFKA_NAMESPACE}" metrics registry keycloak; do
    kubectl get pods -n "${ns}" >/dev/null 2>&1 && { echo "[${ns}]"; kubectl get pods -n "${ns}" --no-headers 2>/dev/null | sed 's/^/  /'; }
  done
  echo ""
  cmd_urls
}

cmd_urls() {
  load_state
  local d="${CONSOLE_CLUSTER_DOMAIN}"
  # Is the host-run API (frontend/backend mode) currently up on :8080?
  local api_state="run 'dev.sh backend' or 'dev.sh frontend' first"
  if curl -s -m 2 -o /dev/null "http://localhost:8080/" 2>/dev/null; then
    api_state="live now"
  fi
  # Is a console deployed in-cluster (operator mode)?
  local op_state="run 'dev.sh operator' first"
  if kubectl get console -A >/dev/null 2>&1 && [ -n "$(kubectl get console -A --no-headers 2>/dev/null)" ]; then
    op_state="live now"
  fi

  info "Console (only reachable while its mode is running)"
  echo "  frontend/backend (runs on your host):  http://localhost:8080          [${api_state}]"
  echo "  operator (runs in the cluster):        https://example-console.${d}   [${op_state}]"
  echo ""
  info "Supporting services (up whenever the environment is)"
  has_profile metrics  && echo "  prometheus:              http://prometheus.${d}"
  has_profile registry && echo "  apicurio registry (UI):  http://registry.${d}/ui"
  has_profile keycloak && echo "  keycloak (admin/admin):  http://keycloak.${d}  (users: admin-user/admin123, dev-user/dev123)"
  has_profile connect  && echo "  kafka connect REST:      http://connect.${d}"
}

cmd_down() {
  local keep="false"
  while [ $# -gt 0 ]; do
    case "$1" in
      --keep-cluster) keep="true"; shift ;;
      -h|--help) echo "Usage: dev.sh down [--keep-cluster]"; exit 0 ;;
      *) die "Unknown option for 'down': $1" ;;
    esac
  done
  if [ "${keep}" = "true" ]; then
    teardown_keep_cluster
  else
    teardown_full
  fi
  info "Done."
}

# --- Dispatch ---------------------------------------------------------------
main() {
  local cmd="${1:-}"
  [ $# -gt 0 ] && shift || true
  case "${cmd}" in
    up)        cmd_up "$@" ;;
    config)    cmd_config "$@" ;;
    backend)   cmd_backend "$@" ;;
    frontend)  cmd_frontend "$@" ;;
    operator)  cmd_operator "$@" ;;
    status)    cmd_status "$@" ;;
    urls)      cmd_urls "$@" ;;
    down)      cmd_down "$@" ;;
    ""|-h|--help|help) usage ;;
    *) err "Unknown command: ${cmd}"; echo ""; usage; exit 1 ;;
  esac
}

main "$@"
