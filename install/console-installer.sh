#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1}"
export CLUSTER_DOMAIN="${2}"

source ${CONSOLE_INSTALL_PATH}/_common.sh

PROVIDED_APIS="$(kubectl get operatorgroup -n ${NAMESPACE} -o json | jq -r '.items[].metadata.annotations["olm.providedAPIs"]' | tr '[:upper:]' '[:lower:]')"

if ! kubectl get crd kafkas.kafka.strimzi.io ; then
    echo -e "${ERROR} Strimzi Kafka Operator custom resource(s) not found"
    display_suggested_subscription "strimzi-kafka-operator" "strimzi-kafka-operator|amq-streams"
    exit 1
fi

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
