#!/bin/bash

set -Eeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

export CONSOLE_OPERATOR_BUNDLE="${1:-}"
export USE_HTTP="${2:-false}"

source ${SCRIPT_PATH}/common.sh
OPERATOR_PATH="$(cd -- "${SCRIPT_PATH}/.." >/dev/null 2>&1 ; pwd -P)"
CATALOG_PATH=${OPERATOR_PATH}/target/catalog

echo "[INFO] Generate catalog in ${CATALOG_PATH}"
rm -rvf ${CATALOG_PATH}
mkdir -p ${CATALOG_PATH}
cp -v ${OPERATOR_PATH}/src/main/olm/*.yaml ${CATALOG_PATH}/
${YQ} -i '.icon = { "base64data": "'$(base64 -w0 ${OPERATOR_PATH}/src/main/olm/icon.png)'", "mediatype": "image/png" }' ${CATALOG_PATH}/package.yaml

for CSV_NAME in $(${YQ} '.entries[].name' ${CATALOG_PATH}/channel.alpha.yaml | sort -V) ; do
    if [ -f ${OPERATOR_PATH}/src/main/olm/bundles/${CSV_NAME}.yaml ] ; then
        BUNDLE_IMAGE=$(${YQ} '.image' ${OPERATOR_PATH}/src/main/olm/bundles/${CSV_NAME}.yaml)
    elif [ -d ${CONSOLE_OPERATOR_BUNDLE} ] ; then
        BUNDLE_IMAGE=${CONSOLE_OPERATOR_BUNDLE}
    else
        BUNDLE_IMAGE=${CONSOLE_OPERATOR_BUNDLE}:$(echo "${CSV_NAME}" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+(-snapshot)?$')
    fi

    RENDER_FLAGS='--output=yaml'

    if [ "${USE_HTTP}" == "true" ] ; then
        RENDER_FLAGS="--use-http ${RENDER_FLAGS}"
    fi

    echo "[INFO] Rendering bundle ${CATALOG_PATH}/${CSV_NAME}.yaml"
    opm render ${BUNDLE_IMAGE} ${RENDER_FLAGS} > ${CATALOG_PATH}/${CSV_NAME}.yaml
done

opm validate ${CATALOG_PATH}

echo "[DEBUG] Catalog generated"
