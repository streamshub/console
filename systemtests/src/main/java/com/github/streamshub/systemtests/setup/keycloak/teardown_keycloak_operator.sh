#!/usr/bin/env bash

set -euo pipefail

if [ -z "${1-}" ]; then echo "ERROR: KEYCLOAK_OPERATOR_NAMESPACE (arg 1) is missing"; exit 1; fi
if [ -z "${2-}" ]; then echo "ERROR: KEYCLOAK_VERSION (arg 2) is missing"; exit 1; fi

KEYCLOAK_OPERATOR_NAMESPACE=$1
KEYCLOAK_VERSION=$2

echo "Delete Keycloak Operator"
kubectl delete -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${KEYCLOAK_VERSION}/kubernetes/kubernetes.yml
kubectl delete -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${KEYCLOAK_VERSION}/kubernetes/keycloaks.k8s.keycloak.org-v1.yml
kubectl delete -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${KEYCLOAK_VERSION}/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml