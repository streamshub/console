
#!/usr/bin/env bash

set -euo pipefail

if [[ -z "${1-}" ]]; then echo "ERROR: KEYCLOAK_OPERATOR_NAMESPACE (arg 1) is missing" >&2; exit 1; fi
if [[ -z "${2-}" ]]; then echo "ERROR: KEYCLOAK_HOSTNAME (arg 2) is missing" >&2; exit 1; fi
if [[ -z "${3-}" ]]; then echo "ERROR: STORE_PASS (arg 3) is missing" >&2; exit 1; fi
if [[ -z "${4-}" ]]; then echo "ERROR: TRUST_STORE_FILE_PATH (arg 4) is missing" >&2; exit 1; fi

KEYCLOAK_OPERATOR_NAMESPACE=$1
KEYCLOAK_HOSTNAME=$2
STORE_PASS=$3
TRUST_STORE_FILE_PATH=$4
ALIAS=keycloak-ca
CERT_FILE=/tmp/keycloak/keycloak-https.crt

echo "Retrieve HTTPS cert from ${KEYCLOAK_HOSTNAME} and export it to ${CERT_FILE}"
openssl s_client -connect ${KEYCLOAK_HOSTNAME}:443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM > ${CERT_FILE}

if [[ ! -s "${CERT_FILE}" ]]; then
  echo "ERROR: Certificate file '${CERT_FILE}' was not created or is empty"
  exit 1
fi
echo "Certificate saved to ${CERT_FILE}"

echo "Checking if truststore ${TRUST_STORE_FILE_PATH} already contains alias ${ALIAS}"
if keytool -list -keystore ${TRUST_STORE_FILE_PATH} -storepass ${STORE_PASS} -alias ${ALIAS} &>/dev/null; then
  echo "Alias ${ALIAS} found in truststore, deleting it"
  keytool -delete -alias ${ALIAS} -keystore ${TRUST_STORE_FILE_PATH} -storepass ${STORE_PASS}
else
  echo "Alias ${ALIAS} not found in truststore. Proceeding with import."
fi

if keytool -list -keystore ${TRUST_STORE_FILE_PATH} -storepass ${STORE_PASS} -alias ${ALIAS} &>/dev/null; then
  echo "ERROR: Alias ${ALIAS} found in truststore after deletion, exiting"
  exit 1
else
  echo "Importing certificate into truststore ${TRUST_STORE_FILE_PATH} with alias ${ALIAS}"
  keytool -import -file ${CERT_FILE} -alias ${ALIAS} -keystore ${TRUST_STORE_FILE_PATH} -storepass ${STORE_PASS} -noprompt
fi

echo "Certificate imported successfully."

echo "Cleanup ${CERT_FILE}"
rm -f ${CERT_FILE}