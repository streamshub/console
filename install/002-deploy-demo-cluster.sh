#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources/kafka

export NAMESPACE="${1?Please provide the deployment namespace}"
export CLUSTER_DOMAIN="${2?Please provide the base domain name for Kafka listener ingress}"

source ${CONSOLE_INSTALL_PATH}/_common.sh

${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/demo-cluster-metrics.configmap.yaml

if [ "$(${KUBE} api-resources --api-group=route.openshift.io -o=name)" != "" ] ; then
    LISTENER_TYPE='route'
else
    LISTENER_TYPE='ingress'
fi

export LISTENER_TYPE

# Replace env variables
${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/demo-cluster.kafka.yaml | ${KUBE} apply -n ${NAMESPACE} -f -

${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/demo-cluster-user1.kafkauser.yaml
