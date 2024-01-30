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

    #echo -e "${AVAILABLE_PKGS}"
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
        yq '.spec.targetNamespaces[0] = strenv(NAMESPACE)' ${RESOURCE_PATH}/console-operators.operatorgroup.yaml \
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

if ! [[ ${PROVIDED_APIS} == *"keycloak.v2alpha1.k8s.keycloak.org"* ]]; then
    echo -e "${WARN} Keycloak Operator is not available for namespace ${NAMESPACE}"
    display_suggested_subscription "keycloak-operator" "keycloak-operator|rhbk-operator"
    INSTALL=false
fi

if ! [[ ${PROVIDED_APIS} == *"prometheus.v1.monitoring.coreos.com"* ]]; then
    echo -e "${WARN} Prometheus Operator is not available for namespace ${NAMESPACE}"
    display_suggested_subscription "prometheus-operator" "prometheus"
    INSTALL=false
fi

assert_installable

echo -e "${INFO} Creating Keycloak PostgreSQL PVC"
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/keycloak-postgres-data.persistentvolumeclaim.yaml

echo -e "${INFO} Creating Keycloak PostgreSQL Credentials"
kubectl create secret generic keycloak-postgres-secret -n ${NAMESPACE} \
    --dry-run=client \
    --from-literal=username='keycloak' \
    --from-literal=password="$(tr -dc A-Za-z0-9 </dev/urandom | head -c 13; echo)" \
    -o yaml | kubectl apply -n ${NAMESPACE} -f -

echo -e "${INFO} Creating Keycloak PostgreSQL StatefulSet"
if kubectl get statefulset keycloak-postgres -n ${NAMESPACE} ; then
    kubectl scale --replicas=0 statefulset/keycloak-postgres -n ${NAMESPACE}
fi
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/keycloak-postgres.statefulset.yaml

echo -e "${INFO} Creating Keycloak PostgreSQL Service"
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/keycloak-postgres.service.yaml

echo -e "${INFO} Creating Keycloak Instance"
if kubectl get statefulset console-keycloak -n ${NAMESPACE} ; then
    kubectl scale --replicas=0 statefulset/console-keycloak -n ${NAMESPACE}
fi
yq '.spec.hostname.hostname = "console-keycloak." + strenv(CLUSTER_DOMAIN)' ${RESOURCE_PATH}/console-keycloak.keycloak.yaml \
    | kubectl apply -n ${NAMESPACE} -f -

CONSOLE_UI_SECRET=$(tr -dc A-Za-z0-9 </dev/urandom | head -c 13; echo)

if kubectl get job console-preview -n ${NAMESPACE} ; then
    kubectl delete job console-preview -n ${NAMESPACE}
fi

yq '
  select(.spec.realm.clients[] | select(.clientId=="console-ui").secret = "'${CONSOLE_UI_SECRET}'") |
  select(.spec.realm.clients[] | select(.clientId=="console-ui").redirectUris += "https://console-ui." + strenv(CLUSTER_DOMAIN) + "/*" ) |
  select(.spec.realm.clients[] | select(.clientId=="kafka").secret = "'$(tr -dc A-Za-z0-9 </dev/urandom | head -c 13; echo)'")
' resources/console-preview.keycloakrealmimport.yaml \
    | kubectl apply -n ${NAMESPACE} -f -

kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-prometheus-server.clusterrole.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-prometheus-server.serviceaccount.yaml
yq '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/console-prometheus-server.clusterrolebinding.yaml \
    | kubectl apply -n ${NAMESPACE} -f -

kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/kafka-resources.podmonitor.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/kubernetes-scrape-configs.secret.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-prometheus.prometheus.yaml

yq '.spec.rules[0].host = "console-prometheus." + strenv(CLUSTER_DOMAIN)' ${RESOURCE_PATH}/console-prometheus.ingress.yaml \
    | kubectl apply -n ${NAMESPACE} -f -

kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-api-server.clusterrole.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-api-server.serviceaccount.yaml
yq '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/console-api-server.clusterrolebinding.yaml \
    | kubectl apply -n ${NAMESPACE} -f -

kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-api.service.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-ui.service.yaml

echo -e "${INFO} Creating Console Credentials"
kubectl create secret generic console-ui-secrets -n ${NAMESPACE} \
    --dry-run=client \
    --from-literal=SESSION_SECRET="$(tr -dc A-Za-z0-9 </dev/urandom | head -c 32; echo)" \
    --from-literal=NEXTAUTH_SECRET="$(tr -dc A-Za-z0-9 </dev/urandom | head -c 32; echo)" \
    --from-literal=KEYCLOAK_CLIENTID="console-ui" \
    --from-literal=KEYCLOAK_CLIENTSECRET="${CONSOLE_UI_SECRET}" \
    -o yaml | kubectl apply -n ${NAMESPACE} -f -

if kubectl get deployment console-api -n ${NAMESPACE} ; then
    kubectl scale --replicas=0 deployment/console-api -n ${NAMESPACE}
fi

# Replace env variables
yq '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/console-api.deployment.yaml \
    | kubectl apply -n ${NAMESPACE} -f -

yq '.spec.rules[0].host = "console-ui." + strenv(CLUSTER_DOMAIN)' ${RESOURCE_PATH}/console-ui.ingress.yaml \
    | kubectl apply -n ${NAMESPACE} -f -
