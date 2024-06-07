#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources/kafka

export NAMESPACE="${1?Please provide the deployment namespace}"
export CLUSTER_DOMAIN="${2?Please provide the base domain name for Kafka listener ingress}"
export MODE="${3:-kraft}"

if [ "${MODE}" != "kraft" ] && [ "${MODE}" != "zk" ] ; then
    echo "Unknown Kafka mode: '${MODE}'. Allowed values are [ 'kraft', 'zk' ]"
    exit 1
fi

source ${CONSOLE_INSTALL_PATH}/_common.sh

${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-kafka-metrics.configmap.yaml

if [ "$(${KUBE} api-resources --api-group=route.openshift.io -o=name)" != "" ] ; then
    LISTENER_TYPE='route'
else
    LISTENER_TYPE='ingress'
fi

export LISTENER_TYPE

if [ "${MODE}" == "kraft" ] ; then
    if ! ${KUBE} get KafkaNodePool console-nodepool >/dev/null ; then
        ${KUBE} delete Kafka console-kafka -n ${NAMESPACE} || true
    fi

    # Replace env variables
    ${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/console-kafka.kafka.yaml | ${KUBE} apply -n ${NAMESPACE} -f -
    ${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-nodepool.kafkanodepool.yaml
else
    if ${KUBE} get KafkaNodePool console-nodepool >/dev/null ; then
        ${KUBE} delete Kafka console-kafka -n ${NAMESPACE} || true
        ${KUBE} delete KafkaNodePool console-nodepool -n ${NAMESPACE} || true
    fi

    # Replace env variables
    ${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/console-kafka-zk.kafka.yaml | ${KUBE} apply -n ${NAMESPACE} -f -
fi

${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/console-kafka-user1.kafkauser.yaml
