#!/bin/bash

set -euo pipefail

CONSOLE_INSTALL_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
RESOURCE_PATH=${CONSOLE_INSTALL_PATH}/resources

export NAMESPACE="${1}"
source ${CONSOLE_INSTALL_PATH}/_common.sh

if ! kubectl get crd prometheuses.monitoring.coreos.com ; then
    echo -e "${ERROR} Prometheus Operator custom resource(s) not found"
    display_suggested_subscription "prometheus-operator" "prometheus"
    exit 1
fi

### Prometheus
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus-server.clusterrole.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus-server.serviceaccount.yaml
yq '.subjects[0].namespace = strenv(NAMESPACE)' ${RESOURCE_PATH}/prometheus/console-prometheus-server.clusterrolebinding.yaml | kubectl apply -n ${NAMESPACE} -f -
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/kafka-resources.podmonitor.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/kubernetes-scrape-configs.secret.yaml
kubectl apply -n ${NAMESPACE} -f ${RESOURCE_PATH}/prometheus/console-prometheus.prometheus.yaml
