#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1}"
source ${CONSOLE_INSTALL_PATH}/_common.sh

echo -e "${INFO} Creating/updating operator group in namespace '${NAMESPACE}'"
yq '.spec.targetNamespaces[0] = strenv(NAMESPACE)' ${RESOURCE_PATH}/prometheus/console-operators.operatorgroup.yaml | kubectl apply -n ${NAMESPACE} -f -

echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: strimzi-kafka-operator
spec:
  name: strimzi-kafka-operator
  channel: stable
  source: community-operators
  sourceNamespace: openshift-marketplace" \
 | kubectl apply -n ${NAMESPACE} -f -

echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: prometheus-operator
spec:
  name: prometheus
  channel: beta
  source: community-operators
  sourceNamespace: openshift-marketplace" \
 | kubectl apply -n ${NAMESPACE} -f -
