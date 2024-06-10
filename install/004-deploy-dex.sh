#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources/dex

export NAMESPACE="${1?Please provide the deployment namespace}"
export CLUSTER_DOMAIN="${2?Please provide the base domain name for Kafka listener ingress}"

source ${CONSOLE_INSTALL_PATH}/_common.sh

# Replace env variables
export CONSOLE_HOSTNAME="console-ui.${CLUSTER_DOMAIN}"

# Replace env variables
${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/dex-rbac.yaml | ${KUBE} apply -n ${NAMESPACE} -f -

echo -e "${INFO} Creating DEX Credentials"
${KUBE} create secret generic console-dex-secrets -n ${NAMESPACE} \
    --dry-run=client \
    --from-literal=DEX_CLIENT_ID="system:serviceaccount:${NAMESPACE}:dex" \
    --from-literal=DEX_CLIENT_SECRET="$(kubectl create token -n ${NAMESPACE} dex --duration=$((365*24))h)" \
    -o yaml | ${KUBE} apply -n ${NAMESPACE} -f -

if ${KUBE} get deployment dex -n ${NAMESPACE} 1>/dev/null 2>&1 ; then
    ${KUBE} scale --replicas=0 deployment/dex -n ${NAMESPACE}
fi

# Replace env variables
${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/dex.yaml | ${KUBE} apply -n ${NAMESPACE} -f -
