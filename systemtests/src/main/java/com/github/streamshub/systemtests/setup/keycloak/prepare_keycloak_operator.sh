#!/usr/bin/env bash

set -euo pipefail

if [[ -z "${1-}" ]]; then echo "ERROR: KEYCLOAK_OPERATOR_NAMESPACE (arg 1) is missing" >&2; exit 1; fi
if [[ -z "${2-}" ]]; then echo "ERROR: KEYCLOAK_VERSION (arg 2) is missing" >&2; exit 1; fi
if [[ -z "${3-}" ]]; then echo "ERROR: KEYCLOAK_HOSTNAME (arg 3) is missing" >&2; exit 1; fi

KEYCLOAK_OPERATOR_NAMESPACE=$1
KEYCLOAK_VERSION=$2
KEYCLOAK_HOSTNAME=$3
TLS_SECRET=$4

KEYCLOAK_PRIVATE_KEY=/tmp/keycloak/key.pem
KEYCLOAK_CERT=/tmp/keycloak/certificate.pem

echo "Prepare fresh keycloak folder for certs"
rm -rf /tmp/keycloak
mkdir -p /tmp/keycloak

echo "Generate certificate with CN=${KEYCLOAK_HOSTNAME} to ${KEYCLOAK_CERT} and private key to ${KEYCLOAK_PRIVATE_KEY}"
openssl req -subj "/CN=${KEYCLOAK_HOSTNAME}/O=StreamsHubKeycloak/C=US" -newkey rsa:2048 -nodes -keyout ${KEYCLOAK_PRIVATE_KEY} -x509 -days 365 -out ${KEYCLOAK_CERT}

echo "Create TLS secret from the cert and private key in namespace ${KEYCLOAK_OPERATOR_NAMESPACE}"
kubectl create secret tls ${TLS_SECRET} --cert ${KEYCLOAK_CERT} --key ${KEYCLOAK_PRIVATE_KEY} -n ${KEYCLOAK_OPERATOR_NAMESPACE}

kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${KEYCLOAK_VERSION}/kubernetes/keycloaks.k8s.keycloak.org-v1.yml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${KEYCLOAK_VERSION}/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml
curl -s https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${KEYCLOAK_VERSION}/kubernetes/kubernetes.yml | sed "s#namespace: .*#namespace: ${KEYCLOAK_OPERATOR_NAMESPACE}#g" | kubectl apply  -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f -

echo "Cleanup ${KEYCLOAK_CERT} ; ${KEYCLOAK_PRIVATE_KEY}"
rm -f ${KEYCLOAK_PRIVATE_KEY} ${KEYCLOAK_CERT}