#!/bin/bash

set -Eeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
OPERATOR_PATH="$(cd -- "${SCRIPT_PATH}/.." >/dev/null 2>&1 ; pwd -P)"
source ${SCRIPT_PATH}/common.sh
CATALOG_PATH=${OPERATOR_PATH}/target/catalog

channels() {
    local CSV_NAME=${1}
    ${YQ} -N 'select(.entries[].name == "'${CSV_NAME}'") | .name' ${CATALOG_PATH}/channel.*.yaml | sort -Vr | tr '\n' ',' | sed 's/,$/\n/'
}

replaces() {
    local CSV_NAME=${1}
    ${YQ} '.entries[] | select(.name == "'${CSV_NAME}'") | .replaces' ${CATALOG_PATH}/channel.alpha.yaml
}

main () {
    COMMUNITY_CATALOG_PATH=${OPERATOR_PATH}/target/community-catalog
    echo "[INFO ] Generating community-operators catalog in ${COMMUNITY_CATALOG_PATH}"

    CONTAINER_RUNTIME=$(which podman || which docker)
    rm -rvf ${COMMUNITY_CATALOG_PATH}
    mkdir -p ${COMMUNITY_CATALOG_PATH}

    for CSV_NAME in $(${YQ} '.entries[].name' ${CATALOG_PATH}/channel.alpha.yaml | sort -V) ; do
        if [ -f ${OPERATOR_PATH}/src/main/olm/bundles/${CSV_NAME}.yaml ] ; then
            BUNDLE_IMAGE=$(${YQ} '.image' ${OPERATOR_PATH}/src/main/olm/bundles/${CSV_NAME}.yaml)
        else
            echo "No bundle image found for CSV ${CSV_NAME}, skipping"
            continue
        fi

        VERSION=$(echo "${CSV_NAME}" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+(-snapshot)?$')
        echo "[INFO ] Generating community-operators bundle ${VERSION}"

        ${CONTAINER_RUNTIME} pull ${BUNDLE_IMAGE}
        CONTAINER_ID=$(${CONTAINER_RUNTIME} create ${BUNDLE_IMAGE})
        mkdir -v ${COMMUNITY_CATALOG_PATH}/${VERSION}
        ${CONTAINER_RUNTIME} cp ${CONTAINER_ID}:/manifests ${COMMUNITY_CATALOG_PATH}/${VERSION}
        ${CONTAINER_RUNTIME} cp ${CONTAINER_ID}:/metadata ${COMMUNITY_CATALOG_PATH}/${VERSION}
        ${CONTAINER_RUNTIME} rm ${CONTAINER_ID}
        ${CONTAINER_RUNTIME} image rm ${BUNDLE_IMAGE}

        CSV_FILE_PATH="${COMMUNITY_CATALOG_PATH}/${VERSION}/manifests/*.clusterserviceversion.yaml"
        ${YQ} -i '.spec.replaces = "'$(replaces ${CSV_NAME})'"' ${CSV_FILE_PATH}
        ${YQ} -i '.spec.icon = [{ "base64data": "'$(base64 -w0 ${SCRIPT_PATH}/../src/main/olm/icon.png)'", "mediatype": "image/png" }]' ${CSV_FILE_PATH}
        ${YQ} -i '.annotations["operators.operatorframework.io.bundle.channels.v1"] = "'$(channels ${CSV_NAME})'"' ${COMMUNITY_CATALOG_PATH}/${VERSION}/metadata/annotations.yaml
        operator-sdk bundle validate "${COMMUNITY_CATALOG_PATH}/${VERSION}" --select-optional name=operatorhub
    done

    echo "[DEBUG] Catalog generated"
}

main
