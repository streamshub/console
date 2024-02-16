#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color
INFO="[ \033[38;5;33mINFO${NC} ]"
WARN="[ \033[38;5;208mWARN${NC} ]"
ERROR="[ \033[38;5;196mERROR${NC} ]"
INSTALL=true

function assert_installable {
    if [ ${INSTALL} == false ] ; then
        echo -e "${ERROR} Process incomplete - exiting"
        exit 1
    fi
}

function fetch_available_packages {
    local NAME_PATTERN="${1}"

    for pm in $(kubectl get packagemanifests -o name | grep -Pe '^packagemanifest\.packages\.operators\.coreos\.com/('"${NAME_PATTERN}"')$') ; do
        kubectl get $pm -o yaml | yq -o=json '{
            "name": .status.packageName,
            "channel": .status.defaultChannel,
            "catalogSource": .status.catalogSource,
            "catalogSourceNamespace": .status.catalogSourceNamespace
        }'
    done | yq ea -p=json '[.]' | yq -o=csv | tail -n +2
}

function display_suggested_subscription {
    local OPERATOR_NAME="${1}"
    local NAME_PATTERN="${2}"

    local AVAILABLE_PKGS="$(fetch_available_packages "${NAME_PATTERN}")"
    echo -e "${INFO} ${OPERATOR_NAME} may be installed by creating one of the following resources:"
    COUNTER=0

    while IFS=, read -r PKG_NAME PKG_CHANNEL PKG_CTLG_SRC PKG_CTLG_SRC_NS; do
        COUNTER=$(( COUNTER + 1 ))
        echo -e "${INFO} ----- Option ${COUNTER} -----"
        echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: ${OPERATOR_NAME}
  namespace: ${NAMESPACE}
spec:
  name: ${PKG_NAME}
  channel: ${PKG_CHANNEL}
  source: ${PKG_CTLG_SRC}
  sourceNamespace: ${PKG_CTLG_SRC_NS}" | yq
    done < <(echo "${AVAILABLE_PKGS}")
}

export NAMESPACE="${1}"
export CLUSTER_DOMAIN="${2}"

if kubectl get namespace/${NAMESPACE} >/dev/null 2>&1 ; then
    echo -e "${INFO} Namespace '${NAMESPACE}' found"
else
    echo -e "${WARN} Namespace '${NAMESPACE}' not found... creating"
    kubectl create namespace ${NAMESPACE} >/dev/null

    if kubectl get namespace/${NAMESPACE} >/dev/null 2>&1 ; then
        echo -e "${INFO} Namespace '${NAMESPACE}' created"
        echo -e "${INFO} Creating operator group in namespace '${NAMESPACE}'"
        yq '.spec.targetNamespaces[0] = strenv(NAMESPACE)' ${RESOURCE_PATH}/prometheus/console-operators.operatorgroup.yaml \
            | kubectl apply -n ${NAMESPACE} -f -
    else
        echo -e "${WARN} Namespace '${NAMESPACE}' could not be created"
    fi
fi

PROVIDED_APIS="$(kubectl get operatorgroup -n ${NAMESPACE} -o json | jq -r '.items[].metadata.annotations["olm.providedAPIs"]' | tr '[:upper:]' '[:lower:]')"

if ! [[ ${PROVIDED_APIS} == *"kafka.v1beta2.kafka.strimzi.io"* ]]; then
    echo -e "${WARN} Strimzi Kafka Operator is not available for namespace ${NAMESPACE}"
    display_suggested_subscription "strimzi-kafka-operator" "strimzi-kafka-operator|amq-streams"
    INSTALL=false
fi

if ! [[ ${PROVIDED_APIS} == *"prometheus.v1.monitoring.coreos.com"* ]]; then
    echo -e "${WARN} Prometheus Operator is not available for namespace ${NAMESPACE}"
    display_suggested_subscription "prometheus-operator" "prometheus"
    INSTALL=false
fi

assert_installable

### Prometheus
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus-server.clusterrole.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus-server.serviceaccount.yaml
yq '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/prometheus/console-prometheus-server.clusterrolebinding.yaml | kubectl apply -n ${NAMESPACE} -f -
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/kafka-resources.podmonitor.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/kubernetes-scrape-configs.secret.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus.prometheus.yaml

### Console
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-api-server.clusterrole.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-api-server.serviceaccount.yaml
yq '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/console/console-api-server.clusterrolebinding.yaml | kubectl apply -n ${NAMESPACE} -f -
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-api.service.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-ui.service.yaml

echo -e "${INFO} Creating Console Credentials"
kubectl create secret generic console-ui-secrets -n ${NAMESPACE} \
    --dry-run=client \
    --from-literal=SESSION_SECRET="$(tr -dc A-Za-z0-9 </dev/urandom | head -c 32; echo)" \
    --from-literal=NEXTAUTH_SECRET="$(tr -dc A-Za-z0-9 </dev/urandom | head -c 32; echo)" \
    -o yaml | kubectl apply -n ${NAMESPACE} -f -

if kubectl get deployment console-api -n ${NAMESPACE} ; then
    kubectl scale --replicas=0 deployment/console-api -n ${NAMESPACE}
fi

if [ "$(kubectl api-resources --api-group=route.openshift.io -o=name)" != "" ] ; then
    kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-ui.route.yaml
    CONSOLE_HOSTNAME=$(kubectl get route console-ui-route -n ${NAMESPACE} -o jsonpath='{.spec.host}')
else
    CONSOLE_HOSTNAME="console-ui.${CLUSTER_DOMAIN}"
    yq '.spec.rules[0].host = strenv(CONSOLE_HOSTNAME)' ${RESOURCE_PATH}/console/console-ui.ingress.yaml | kubectl apply -n ${NAMESPACE} -f -
fi

# Replace env variables
export CONSOLE_HOSTNAME
yq '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/console/console-api.deployment.yaml | kubectl apply -n ${NAMESPACE} -f -
