#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources
DEFAULT_CATALOG='quay.io/streamshub/console-operator-catalog:latest'

export NAMESPACE="${1?Please provide the deployment namespace}"
export CATALOG_IMAGE="${2:-${DEFAULT_CATALOG}}"
source ${CONSOLE_INSTALL_PATH}/_common.sh

echo -e "${INFO} Create/update operator group in namespace '${NAMESPACE}' to watch all namespaces"
${YQ} '.spec.targetNamespaces = []' ${RESOURCE_PATH}/operators/console-operators.operatorgroup.yaml | ${KUBE} apply -n ${NAMESPACE} -f -

echo -e "${INFO} Create/update operator group in namespace '${NAMESPACE}' with catalog '${CATALOG_IMAGE}'"
echo "apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: console-operator-catalog
spec:
  displayName: 'StreamsHub'
  image: '${CATALOG_IMAGE}'
  publisher: 'StreamsHub'
  sourceType: grpc" | ${KUBE} apply -n ${NAMESPACE} -f -

echo -e "${INFO} Create/update subscription in namespace '${NAMESPACE}'"
echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: console-operator
spec:
  name: console-operator
  channel: alpha
  source: console-operator-catalog
  sourceNamespace: ${NAMESPACE}" | ${KUBE} apply -n ${NAMESPACE} -f -
