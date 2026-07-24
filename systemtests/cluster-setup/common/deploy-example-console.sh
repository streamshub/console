#!/usr/bin/env bash
# Deploys Strimzi + a Kafka cluster + the Console operator + a Console
# instance, using the project's own examples/kafka and examples/console
# quickstart manifests. Everything is idempotent (safe to re-run).
#
# Cluster-agnostic — works against whatever cluster context is currently
# active (kind, minikube, or anything else), since it's pure
# kubectl/helm/envsubst.
#
# Requires a CatalogSource to already exist (see setup-catalogsource.sh).
#
# Usage:
#   ./deploy-example-console.sh --catalog-image <image> [options]
#
# Options (all have defaults matching the real systemtests / repo examples):
#   --catalog-image        (required) CatalogSource image, e.g.
#                           quay.io/streamshub/console-operator-catalog:0.12.0
#   --catalog-namespace    default: olm
#   --catalog-name         default: strimzi-source
#   --channel              default: alpha
#   --operator-namespace   default: co-namespace  (Strimzi + Console operator)
#   --strimzi-version      default: 0.51.0        (MUST match the console-operator
#                           build behind --catalog-image — see
#                           ~/claude-docs/streamshub-local-sts/README.md)
#   --kafka-namespace      default: kafka         (Kafka cluster + Console instance)
#   --listener             default: scramplain    (internal listener Console uses)
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

CATALOG_IMAGE=""
CATALOG_NAMESPACE="olm"
CATALOG_NAME="strimzi-source"
CHANNEL="alpha"
OPERATOR_NAMESPACE="co-namespace"
STRIMZI_VERSION="0.51.0"
KAFKA_NAMESPACE="kafka"
KAFKA_NAME="console-kafka"
CONSOLE_NAME="example"
LISTENER="scramplain"
CONSOLE_CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN:-127.0.0.1.nip.io}"

usage() {
  echo "Usage: $0 --catalog-image <image> [--catalog-namespace olm] [--catalog-name strimzi-source]" >&2
  echo "          [--channel alpha] [--operator-namespace co-namespace] [--strimzi-version 0.51.0]" >&2
  echo "          [--kafka-namespace kafka] [--listener scramplain]" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --catalog-image) CATALOG_IMAGE="$2"; shift 2 ;;
    --catalog-namespace) CATALOG_NAMESPACE="$2"; shift 2 ;;
    --catalog-name) CATALOG_NAME="$2"; shift 2 ;;
    --channel) CHANNEL="$2"; shift 2 ;;
    --operator-namespace) OPERATOR_NAMESPACE="$2"; shift 2 ;;
    --strimzi-version) STRIMZI_VERSION="$2"; shift 2 ;;
    --kafka-namespace) KAFKA_NAMESPACE="$2"; shift 2 ;;
    --listener) LISTENER="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown argument: $1" >&2; usage ;;
  esac
done

[ -n "${CATALOG_IMAGE}" ] || usage

# --- 1. CatalogSource (OLM install if needed) ------------------------------
./setup-catalogsource.sh --image "${CATALOG_IMAGE}" --namespace "${CATALOG_NAMESPACE}" --name "${CATALOG_NAME}"

# --- 2. Strimzi via Helm ----------------------------------------------------
kubectl create namespace "${OPERATOR_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

if helm status strimzi-cluster-operator -n "${OPERATOR_NAMESPACE}" >/dev/null 2>&1; then
  echo "Strimzi already installed in '${OPERATOR_NAMESPACE}', skipping"
else
  echo "Installing Strimzi ${STRIMZI_VERSION} into '${OPERATOR_NAMESPACE}'..."
  helm install strimzi-cluster-operator oci://quay.io/strimzi-helm/strimzi-kafka-operator \
    --version "${STRIMZI_VERSION}" \
    --namespace "${OPERATOR_NAMESPACE}" \
    --set watchAnyNamespace=true \
    --wait
fi
kubectl -n "${OPERATOR_NAMESPACE}" wait --for=condition=available deployment/strimzi-cluster-operator --timeout=180s

# --- 3. Console operator: OperatorGroup + Subscription ----------------------
cat <<EOF | kubectl apply -n "${OPERATOR_NAMESPACE}" -f -
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  name: streamshub-operators
spec:
  upgradeStrategy: Default
---
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: console-sub
spec:
  channel: ${CHANNEL}
  installPlanApproval: Automatic
  name: streamshub-console-operator
  source: ${CATALOG_NAME}
  sourceNamespace: ${CATALOG_NAMESPACE}
EOF

echo "Waiting for Subscription to resolve a CSV..."
csv=""
for i in $(seq 1 60); do
  csv=$(kubectl -n "${OPERATOR_NAMESPACE}" get subscription console-sub -o jsonpath='{.status.installedCSV}' 2>/dev/null || true)
  [ -n "${csv}" ] && break
  sleep 5
done
[ -n "${csv}" ] || { echo "Timed out waiting for Subscription to resolve a CSV" >&2; exit 1; }

echo "Waiting for CSV '${csv}' to succeed..."
phase=""
for i in $(seq 1 60); do
  phase=$(kubectl -n "${OPERATOR_NAMESPACE}" get csv "${csv}" -o jsonpath='{.status.phase}' 2>/dev/null || true)
  [ "${phase}" = "Succeeded" ] && break
  if [ "${phase}" = "Failed" ]; then echo "CSV '${csv}' failed" >&2; exit 1; fi
  sleep 5
done
[ "${phase}" = "Succeeded" ] || { echo "Timed out waiting for CSV '${csv}' (last phase: ${phase:-unknown})" >&2; exit 1; }
echo "Console operator installed: ${csv}"

# --- 4. Kafka cluster (examples/kafka/*.yaml + internal listener) ----------
kubectl create namespace "${KAFKA_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

export CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN}"
export NAMESPACE="${KAFKA_NAMESPACE}"
export LISTENER_TYPE="ingress"

echo "Applying examples/kafka/*.yaml (cluster domain: ${CLUSTER_DOMAIN})..."
cat ../../../examples/kafka/*.yaml | envsubst | kubectl apply -n "${KAFKA_NAMESPACE}" -f -

echo "Adding internal '${LISTENER}' listener for in-cluster consumers (e.g. Console)..."
kubectl -n "${KAFKA_NAMESPACE}" patch kafka "${KAFKA_NAME}" --type=json \
  -p="[{\"op\": \"add\", \"path\": \"/spec/kafka/listeners/-\", \"value\": {\"name\": \"${LISTENER}\", \"port\": 9095, \"type\": \"internal\", \"tls\": false, \"authentication\": {\"type\": \"scram-sha-512\"}}}]" \
  2>/dev/null || echo "(listener already present, skipping)"

echo "Waiting for Kafka '${KAFKA_NAME}' to become Ready (can take a few minutes)..."
kubectl -n "${KAFKA_NAMESPACE}" wait --for=condition=Ready "kafka/${KAFKA_NAME}" --timeout=600s

# --- 5. Console instance (examples/console/*.yaml, internal listener) ------
export KAFKA_NAMESPACE

echo "Applying examples/console/010-Console-example.yaml..."
cat ../../../examples/console/010-Console-example.yaml | envsubst | kubectl apply -n "${KAFKA_NAMESPACE}" -f -

echo "Pointing Console at the internal '${LISTENER}' listener (not the example's default 'secure')..."
kubectl -n "${KAFKA_NAMESPACE}" patch console "${CONSOLE_NAME}" --type=json \
  -p="[{\"op\": \"replace\", \"path\": \"/spec/kafkaClusters/0/listener\", \"value\": \"${LISTENER}\"}]"

echo "Waiting for Console '${CONSOLE_NAME}' to become Ready (can take a minute or two)..."
kubectl -n "${KAFKA_NAMESPACE}" wait --for=condition=Ready "console/${CONSOLE_NAME}" --timeout=300s

echo ""
echo "Done. Console ready: https://${CONSOLE_NAME}-console.${CONSOLE_CLUSTER_DOMAIN}"
