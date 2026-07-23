#!/usr/bin/env bash
# Tears down the local setup. Default: deletes the entire kind cluster
# (fastest, cleanest full reset — matches how the environment has been reset
# between test runs so far). Pass --keep-cluster to instead remove just the
# deployed resources (Kafka, Console, Console operator, Strimzi, OLM) while
# leaving the kind cluster + ingress-nginx running, so a subsequent
# common/ script run skips cluster creation and ingress image pulls.
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

if [ "${KEEP_CLUSTER}" = false ]; then
  echo "Deleting kind cluster '${CLUSTER_NAME}' (full teardown)..."
  kind delete cluster --name "${CLUSTER_NAME}"
  rm -f "${CLUSTER_ENV_FILE}"
  echo "Done."
  exit 0
fi

echo "Partial teardown: removing deployed resources, keeping cluster + ingress-nginx..."

# Delete Strimzi/Console custom resources *before* the namespace: if the
# namespace delete tears down the entity-operator pod before it finishes
# clearing a KafkaTopic's `strimzi.io/topic-operator` finalizer, the
# namespace gets stuck in Terminating forever (hit this in testing).
# Deleting the CRs first, while their operator is still running, avoids the
# race.
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
echo "Partial teardown complete. Cluster '${CLUSTER_NAME}' and ingress-nginx are still running."
