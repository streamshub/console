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
#   --catalog-name         default: streamshub-console-catalog
#   --channel              default: alpha
#   --operator-namespace   default: operators       (Strimzi + Console operator)
#   --kafka-namespace      default: console-namespace (Kafka cluster + Console instance)
#   --listener             default: scramplain      (internal listener Console uses, macOS only)
#
# Strimzi installs via an OLM Subscription (channel derived from this repo's
# strimzi-api.version), same mechanism playwright-tests.yml uses — not Helm.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
PROJECT_ROOT="$(cd ../../.. && pwd)"
source ./common.sh

CATALOG_IMAGE=""
CATALOG_NAMESPACE="olm"
CATALOG_NAME="streamshub-console-catalog"
CHANNEL="alpha"
OPERATOR_NAMESPACE="operators"
KAFKA_NAMESPACE="console-namespace"
KAFKA_NAME="console-kafka"
CONSOLE_NAME="example"
LISTENER="scramplain"
CONSOLE_CLUSTER_DOMAIN="${CONSOLE_CLUSTER_DOMAIN:-127.0.0.1.nip.io}"

usage() {
  echo "Usage: $0 --catalog-image <image> [--catalog-namespace olm] [--catalog-name streamshub-console-catalog]" >&2
  echo "          [--channel alpha] [--operator-namespace operators]" >&2
  echo "          [--kafka-namespace console-namespace] [--listener scramplain]" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --catalog-image) CATALOG_IMAGE="$2"; shift 2 ;;
    --catalog-namespace) CATALOG_NAMESPACE="$2"; shift 2 ;;
    --catalog-name) CATALOG_NAME="$2"; shift 2 ;;
    --channel) CHANNEL="$2"; shift 2 ;;
    --operator-namespace) OPERATOR_NAMESPACE="$2"; shift 2 ;;
    --kafka-namespace) KAFKA_NAMESPACE="$2"; shift 2 ;;
    --listener) LISTENER="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown argument: $1" >&2; usage ;;
  esac
done

[ -n "${CATALOG_IMAGE}" ] || usage

# --- 1. CatalogSource (OLM install if needed) ------------------------------
./setup-catalogsource.sh --image "${CATALOG_IMAGE}" --namespace "${CATALOG_NAMESPACE}" --name "${CATALOG_NAME}"

# --- 2. Strimzi via OLM Subscription -----------------------------------------
kubectl create namespace "${OPERATOR_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

STRIMZI_CHANNEL="strimzi-$(mvn help:evaluate -f "${PROJECT_ROOT}/pom.xml" -Dexpression=strimzi-api.version -q -DforceStdout | awk -F. '{print $1"."$2}').x"

if kubectl -n "${OPERATOR_NAMESPACE}" get subscription strimzi-kafka-operator >/dev/null 2>&1; then
  echo "Strimzi Subscription already exists in '${OPERATOR_NAMESPACE}', skipping create"
else
  echo "Installing Strimzi (channel ${STRIMZI_CHANNEL}) into '${OPERATOR_NAMESPACE}'..."
  kubectl apply -n "${OPERATOR_NAMESPACE}" -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: strimzi-kafka-operator
spec:
  channel: ${STRIMZI_CHANNEL}
  name: strimzi-kafka-operator
  source: operatorhubio-catalog
  sourceNamespace: ${CATALOG_NAMESPACE}
EOF
fi

echo "Waiting for Strimzi operator Deployment to appear..."
for i in $(seq 1 60); do
  strimzi_deployment=$(kubectl get deployment --selector=operators.coreos.com/strimzi-kafka-operator.operators -n "${OPERATOR_NAMESPACE}" -o name 2>/dev/null | tail -1)
  [ -n "${strimzi_deployment}" ] && break
  sleep 5
done
[ -n "${strimzi_deployment}" ] || { echo "Timed out waiting for Strimzi operator Deployment to appear" >&2; exit 1; }
kubectl -n "${OPERATOR_NAMESPACE}" wait "${strimzi_deployment}" --for=condition=available --timeout=180s

# --- 3. Console operator: Subscription --------------------------------------
# No OperatorGroup created here deliberately: OLM's own install.sh already
# pre-provisions OPERATOR_NAMESPACE ("operators" by default) with a
# cluster-wide "global-operators" OperatorGroup. Creating a second one in the
# same namespace breaks OLM's CSV resolution for everything already there
# (including Strimzi's) with "TooManyOperatorGroups" - matches how
# playwright-tests.yml already installs the Console operator into
# "operators" with no custom OperatorGroup.
cat <<EOF | kubectl apply -n "${OPERATOR_NAMESPACE}" -f -
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

if [ "${NEEDS_LOCAL_PORT_FORWARD}" = true ]; then
  echo "macOS: adding internal '${LISTENER}' listener for in-cluster consumers (e.g. Console)..."
  kubectl -n "${KAFKA_NAMESPACE}" patch kafka "${KAFKA_NAME}" --type=json \
    -p="[{\"op\": \"add\", \"path\": \"/spec/kafka/listeners/-\", \"value\": {\"name\": \"${LISTENER}\", \"port\": 9095, \"type\": \"internal\", \"tls\": false, \"authentication\": {\"type\": \"scram-sha-512\"}}}]" \
    2>/dev/null || echo "(listener already present, skipping)"
fi

echo "Waiting for Kafka '${KAFKA_NAME}' to become Ready (can take a few minutes)..."
kubectl -n "${KAFKA_NAMESPACE}" wait --for=condition=Ready "kafka/${KAFKA_NAME}" --timeout=600s

# --- 5. Console instance (examples/console/*.yaml) --------------------------
export KAFKA_NAMESPACE

echo "Applying examples/console/010-Console-example.yaml..."
cat ../../../examples/console/010-Console-example.yaml | envsubst | kubectl apply -n "${KAFKA_NAMESPACE}" -f -

if [ "${NEEDS_LOCAL_PORT_FORWARD}" = true ]; then
  echo "macOS: pointing Console at the internal '${LISTENER}' listener (not the example's default 'secure')..."
  kubectl -n "${KAFKA_NAMESPACE}" patch console "${CONSOLE_NAME}" --type=json \
    -p="[{\"op\": \"replace\", \"path\": \"/spec/kafkaClusters/0/listener\", \"value\": \"${LISTENER}\"}]"
else
  echo "Linux: leaving Console on the example's default 'secure' listener (node IP is directly reachable in-cluster)"
fi

echo "Waiting for Console '${CONSOLE_NAME}' to become Ready (can take a minute or two)..."
kubectl -n "${KAFKA_NAMESPACE}" wait --for=condition=Ready "console/${CONSOLE_NAME}" --timeout=300s

echo ""
echo "Done. Console ready: https://${CONSOLE_NAME}-console.${CONSOLE_CLUSTER_DOMAIN}"
