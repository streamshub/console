#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1?Please provide the deployment namespace}"
export CLUSTER_DOMAIN="${2?Please provide the base domain name for Kafka listener ingress}"
export CONSOLE_CONFIG="${3?Please provide the path to a console configuration YAML. See console-config-example.yaml for sample}"

source ${CONSOLE_INSTALL_PATH}/_common.sh

if [ "$(${KUBE} api-resources --api-group=route.openshift.io -o=name 2>/dev/null)" == "" ] ; then
    export CLUSTER_DOMAIN="${2?Please provide the base domain name for console ingress}"
fi

PROVIDED_APIS="$(${KUBE} get operatorgroup -n ${NAMESPACE} -o json | jq -r '.items[].metadata.annotations["olm.providedAPIs"]' | tr '[:upper:]' '[:lower:]')"

if ! ${KUBE} get crd kafkas.kafka.strimzi.io 1>/dev/null 2>&1 ; then
    echo -e "${ERROR} Strimzi Kafka Operator custom resource(s) not found"
    display_suggested_subscription "strimzi-kafka-operator" "strimzi-kafka-operator|amq-streams"
    exit 1
fi

### Console
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-server.clusterrole.yaml
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-server.serviceaccount.yaml
${YQ} '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/console/console-server.clusterrolebinding.yaml | ${KUBE} apply -n ${NAMESPACE} -f -
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-ui.service.yaml

if ! ${KUBE} get secret console-ui-secrets -n ${NAMESPACE} 1>/dev/null 2>&1 ; then
    echo -e "${INFO} Creating Console Credentials"
    ${KUBE} create secret generic console-ui-secrets -n ${NAMESPACE} \
        --dry-run=client \
        --from-literal=SESSION_SECRET="$(LC_CTYPE=C tr -dc A-Za-z0-9 </dev/urandom | head -c 32; echo)" \
        --from-literal=NEXTAUTH_SECRET="$(LC_CTYPE=C tr -dc A-Za-z0-9 </dev/urandom | head -c 32; echo)" \
        -o yaml | ${KUBE} apply -n ${NAMESPACE} -f -
else
    echo -e "${WARN} Console Credential secret console-ui-secrets already exists, nothing applied"
fi

${KUBE} create secret generic console-config -n ${NAMESPACE} \
    --dry-run=client \
    --from-file=console-config.yaml="${CONSOLE_CONFIG}" \
    -o yaml | ${KUBE} apply -n ${NAMESPACE} -f -

if ${KUBE} get deployment console -n ${NAMESPACE} 1>/dev/null 2>&1 ; then
    ${KUBE} scale --replicas=0 deployment/console -n ${NAMESPACE}
fi

if [ "$(${KUBE} api-resources --api-group=route.openshift.io -o=name)" != "" ] ; then
    ${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console/console-ui.route.yaml
    CONSOLE_HOSTNAME=$(${KUBE} get route console-ui-route -n ${NAMESPACE} -o jsonpath='{.spec.host}')
else
    CONSOLE_HOSTNAME="console-ui.${CLUSTER_DOMAIN}"
    ${YQ} '.spec.rules[0].host = strenv(CONSOLE_HOSTNAME)' ${RESOURCE_PATH}/console/console-ui.ingress.yaml | ${KUBE} apply -n ${NAMESPACE} -f -
fi

# Replace env variables
export CONSOLE_HOSTNAME
${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/console/console.deployment.yaml | ${KUBE} apply -n ${NAMESPACE} -f -

echo -e "${INFO} Console deployed and available at https://${CONSOLE_HOSTNAME}"
