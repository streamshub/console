#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1}"
export CLUSTER_DOMAIN="${2}"

kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/cluster-example-metrics.configmap.yaml

if [ "$(kubectl api-resources --api-group=route.openshift.io -o=name)" != "" ] ; then
    LISTENER_TYPE='route'
else
    LISTENER_TYPE='ingress'
fi

export LISTENER_TYPE

# Replace env variables
yq '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/cluster-example.kafka.yaml \
    | kubectl apply -n ${NAMESPACE} -f -
