#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1?Please provide the deployment namespace}"
source ${CONSOLE_INSTALL_PATH}/_common.sh

echo -e "${INFO} Create/update operator group in namespace '${NAMESPACE}'"
${YQ} '.spec.targetNamespaces[0] = strenv(NAMESPACE)' ${RESOURCE_PATH}/prometheus/console-operators.operatorgroup.yaml | ${KUBE} apply -n ${NAMESPACE} -f -

if ${KUBE} get packagemanifests amq-streams >/dev/null ; then
    echo -e "${INFO} Create/update AMQ Streams Kafka Operator"
    echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: amq-streams-operator
spec:
  name: amq-streams
  channel: stable
  source: redhat-operators
  sourceNamespace: openshift-marketplace" | ${KUBE} apply -n ${NAMESPACE} -f -
else
    echo -e "${INFO} Create/update Strimzi Kafka Operator"
    echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: strimzi-kafka-operator
spec:
  name: strimzi-kafka-operator
  channel: stable
  source: community-operators
  sourceNamespace: openshift-marketplace" | ${KUBE} apply -n ${NAMESPACE} -f -
fi

echo -e "${INFO} Create/update Prometheus Operator"
echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: prometheus-operator
spec:
  name: prometheus
  channel: beta
  source: community-operators
  sourceNamespace: openshift-marketplace" \
 | ${KUBE} apply -n ${NAMESPACE} -f -
