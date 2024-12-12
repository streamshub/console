#!/bin/bash

set -xEeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

export CONSOLE_OPERATOR_BUNDLE_IMAGE="${1:-}"

source ${SCRIPT_PATH}/common.sh
OPERATOR_PATH="$(cd -- "${SCRIPT_PATH}/.." >/dev/null 2>&1 ; pwd -P)"
CATALOG_PATH=${OPERATOR_PATH}/target/catalog

echo "[INFO] Generate catalog in ${CATALOG_PATH}"
rm -rvf ${CATALOG_PATH}
mkdir -p ${CATALOG_PATH}
cp -v ${OPERATOR_PATH}/src/main/olm/*.yaml ${CATALOG_PATH}/

for CSV_NAME in $(${YQ} '.entries[].name' ${CATALOG_PATH}/channel.alpha.yaml | sort -V) ; do
    if [ -f ${OPERATOR_PATH}/src/main/olm/bundles/${CSV_NAME}.yaml ] ; then
        BUNDLE_IMAGE=$(${YQ} '.image' ${OPERATOR_PATH}/src/main/olm/bundles/${CSV_NAME}.yaml)
    else
        BUNDLE_IMAGE=${CONSOLE_OPERATOR_BUNDLE_IMAGE}:$(echo "${CSV_NAME}" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+(-snapshot)?$')
    fi

    opm render ${BUNDLE_IMAGE} --output=yaml > ${CATALOG_PATH}/${CSV_NAME}.yaml
done

opm validate ${CATALOG_PATH}

echo "[DEBUG] Catalog generated"
