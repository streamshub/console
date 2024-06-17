#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1?Please provide the deployment namespace}"
export CLUSTER_DOMAIN="${2:-}"
source ${CONSOLE_INSTALL_PATH}/_common.sh

if ! ${KUBE} get crd prometheuses.monitoring.coreos.com >/dev/null ; then
    echo -e "${ERROR} Prometheus Operator custom resource(s) not found"
    display_suggested_subscription "prometheus-operator" "prometheus"
    exit 1
fi

### Prometheus
echo -e "${INFO} Apply Prometheus security resources"
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus-server.clusterrole.yaml
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus-server.serviceaccount.yaml
${YQ} '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/prometheus/console-prometheus-server.clusterrolebinding.yaml | ${KUBE} apply -n ${NAMESPACE} -f -

echo -e "${INFO} Apply Prometheus PodMonitor and Kubernetes scrape configurations"
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/kafka-resources.podmonitor.yaml
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/kubernetes-scrape-configs.secret.yaml

echo -e "${INFO} Apply Prometheus instance"
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus.prometheus.yaml
${KUBE} apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus.service.yaml

if [ -n "${CLUSTER_DOMAIN}" ] ; then
    # Replace env variables
    ${YQ} '(.. | select(tag == "!!str")) |= envsubst(ne)' ${RESOURCE_PATH}/prometheus/console-prometheus.ingress.yaml | ${KUBE} apply -n ${NAMESPACE} -f -
fi
