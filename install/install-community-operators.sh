#!/bin/bash

set -euo pipefail

export NAMESPACE="${1}"

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
