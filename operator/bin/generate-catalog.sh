#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
source ${SCRIPT_PATH}/common.sh
export VERSION="${1}"

echo "[INFO] Generate catalog"

rm -rvf ${CATALOG_PATH} ${CATALOG_PATH}.Dockerfile
mkdir -p ${CATALOG_PATH}

opm generate dockerfile ${CATALOG_PATH}
opm init ${OPERATOR_NAME} --default-channel=alpha --output=yaml > ${CATALOG_PATH}/operator.yaml
opm render ${BUNDLE_PATH} --output=yaml >> ${CATALOG_PATH}/operator.yaml

cat << EOF >> ${CATALOG_PATH}/operator.yaml
---
schema: olm.channel
package: ${OPERATOR_NAME}
name: alpha
entries:
  - name: ${OPERATOR_CSV_NAME}
EOF

# Rename package names
${YQ} eval -o yaml -i ". |= select(.package == \"${ORIGINAL_OPERATOR_NAME}\").package = \"${OPERATOR_NAME}\"" "${CATALOG_PATH}/operator.yaml"
${YQ} eval -o yaml -i "(.properties[] | select(.value.packageName == \"${ORIGINAL_OPERATOR_NAME}\") | .value.packageName) = \"${OPERATOR_NAME}\"" "${CATALOG_PATH}/operator.yaml"

opm validate ${CATALOG_PATH}

echo "[DEBUG] Catalog generated"