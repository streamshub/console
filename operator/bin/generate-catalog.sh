#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

export VERSION="${1}"

source ${SCRIPT_PATH}/common.sh

echo "[INFO] Generate catalog"

rm -rvf ${CATALOG_PATH} ${CATALOG_PATH}.Dockerfile
mkdir -p ${CATALOG_PATH}

opm generate dockerfile ${CATALOG_PATH}
opm init ${OPERATOR_NAME} --default-channel=alpha --output=yaml > ${OPERATOR_CATALOG_CONFIG_YAML_PATH}
opm render ${BUNDLE_PATH} --output=yaml >> ${OPERATOR_CATALOG_CONFIG_YAML_PATH}

cat << EOF >> ${OPERATOR_CATALOG_CONFIG_YAML_PATH}
---
schema: olm.channel
package: ${OPERATOR_NAME}
name: alpha
entries:
  - name: ${OPERATOR_CSV_NAME}
EOF

# Rename package names
${YQ} eval -o yaml -i ". |= select(.package == \"${ORIGINAL_OPERATOR_NAME}\").package = \"${OPERATOR_NAME}\"" "${OPERATOR_CATALOG_CONFIG_YAML_PATH}"
${YQ} eval -o yaml -i "(.properties[] | select(.value.packageName == \"${ORIGINAL_OPERATOR_NAME}\") | .value.packageName) = \"${OPERATOR_NAME}\"" "${OPERATOR_CATALOG_CONFIG_YAML_PATH}"

opm validate ${CATALOG_PATH}

echo "[DEBUG] Catalog generated"