#!/usr/bin/env bash
# Tears down the local minikube setup. Always stops the background
# port-forward or tunnel first. Default: deletes the entire minikube
# profile. Pass --keep-cluster to instead remove just the deployed
# resources (Kafka, Console, Console operator, Strimzi, OLM) while leaving
# the minikube profile + ingress-nginx running.
#
# Usage:
#   ./delete-cluster.sh                    # delete the whole cluster
#   ./delete-cluster.sh --keep-cluster      # remove deployed resources only
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib/env.sh

KEEP_CLUSTER=false
KAFKA_NAMESPACE="kafka"
OPERATOR_NAMESPACE="co-namespace"

while [ $# -gt 0 ]; do
  case "$1" in
    --keep-cluster) KEEP_CLUSTER=true; shift ;;
    --kafka-namespace) KAFKA_NAMESPACE="$2"; shift 2 ;;
    --operator-namespace) OPERATOR_NAMESPACE="$2"; shift 2 ;;
    -h|--help)
      echo "Usage: $0 [--keep-cluster] [--kafka-namespace kafka] [--operator-namespace co-namespace]" >&2
      exit 1
      ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if [ -f "${PORT_FORWARD_PID_FILE}" ]; then
  pid=$(cat "${PORT_FORWARD_PID_FILE}")
  if kill "${pid}" 2>/dev/null; then
    echo "Stopped port-forward (pid ${pid})"
  fi
  rm -f "${PORT_FORWARD_PID_FILE}" "${PORT_FORWARD_LOG_FILE}"
fi

if [ -f "${TUNNEL_PID_FILE}" ]; then
  pid=$(cat "${TUNNEL_PID_FILE}")
  # The tunnel runs as root (via sudo), so a plain `kill` from this
  # unprivileged script can't touch it. Try non-interactively with cached
  # sudo credentials first; if that's not available, ask the user.
  #
  # Deliberately does NOT remove .tunnel.pid if the kill fails: this file is
  # the only way create-cluster.sh (in this or a future run) can detect that
  # an orphaned root-owned tunnel is still alive and squatting on ports
  # 80/443 — losing that tracking is exactly what caused a real, confusing
  # failure once already (an old tunnel outlived a cluster recreation,
  # silently occupying the ports while routing to nothing, while a fresh
  # cluster's Console showed backend errors that looked network-related but
  # weren't).
  if sudo -n kill "${pid}" 2>/dev/null; then
    echo "Stopped tunnel (pid ${pid})"
    rm -f "${TUNNEL_PID_FILE}" "${TUNNEL_LOG_FILE}"
  else
    echo "Could not stop the root-owned tunnel process (pid ${pid}) without a sudo prompt." >&2
    echo "Run 'sudo kill ${pid}' yourself, or Ctrl-C the terminal it's running in — it will" >&2
    echo "otherwise keep running (and holding ports 80/443) even after this cluster is gone." >&2
  fi
fi

if [ "${KEEP_CLUSTER}" = false ]; then
  echo "Deleting minikube profile '${MINIKUBE_PROFILE}' (full teardown)..."
  minikube delete -p "${MINIKUBE_PROFILE}"
  rm -f "${CLUSTER_ENV_FILE}"
  echo "Done."
  exit 0
fi

echo "Partial teardown: removing deployed resources, keeping cluster + ingress-nginx..."

# Delete Strimzi/Console custom resources *before* the namespace: if the
# namespace delete tears down the entity-operator pod before it finishes
# clearing a KafkaTopic's `strimzi.io/topic-operator` finalizer, the
# namespace gets stuck in Terminating forever (same issue hit and fixed in
# ../kind/delete-cluster.sh).
echo "Deleting Kafka/Console custom resources in '${KAFKA_NAMESPACE}' (avoids a finalizer race with namespace deletion)..."
kubectl delete console,kafkatopic,kafkauser,kafka,kafkanodepool -n "${KAFKA_NAMESPACE}" --all --ignore-not-found --timeout=120s || true

echo "Deleting namespace '${KAFKA_NAMESPACE}' (Kafka + Console instance)..."
kubectl delete namespace "${KAFKA_NAMESPACE}" --ignore-not-found --timeout=120s || {
  echo "Namespace '${KAFKA_NAMESPACE}' didn't terminate in time — clearing any stuck finalizers and retrying..." >&2
  for r in $(kubectl get kafkatopic,kafkauser,kafka -n "${KAFKA_NAMESPACE}" -o name 2>/dev/null); do
    kubectl patch "${r}" -n "${KAFKA_NAMESPACE}" --type=merge -p '{"metadata":{"finalizers":[]}}' 2>/dev/null || true
  done
  kubectl delete namespace "${KAFKA_NAMESPACE}" --ignore-not-found
}

echo "Deleting namespace '${OPERATOR_NAMESPACE}' (Strimzi + Console operator + Subscription)..."
kubectl delete namespace "${OPERATOR_NAMESPACE}" --ignore-not-found

if kubectl get crd catalogsources.operators.coreos.com >/dev/null 2>&1; then
  if command -v operator-sdk >/dev/null 2>&1; then
    echo "Uninstalling OLM via operator-sdk..."
    operator-sdk olm uninstall
  else
    echo "OLM is installed but 'operator-sdk' isn't available to cleanly uninstall it." >&2
    echo "Install it (e.g. 'brew install operator-sdk') and re-run with --keep-cluster," >&2
    echo "or run without --keep-cluster to delete the whole cluster instead." >&2
    exit 1
  fi
fi

echo ""
echo "Partial teardown complete. Cluster '${MINIKUBE_PROFILE}' and ingress-nginx are still running."
