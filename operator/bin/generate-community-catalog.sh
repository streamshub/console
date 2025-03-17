#!/bin/bash

set -xEeuo pipefail

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

        ${CONTAINER_RUNTIME} pull ${BUNDLE_IMAGE}
        CONTAINER_ID=$(${CONTAINER_RUNTIME} create ${BUNDLE_IMAGE})
        VERSION=$(echo "${CSV_NAME}" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+(-snapshot)?$')
        mkdir -v ${COMMUNITY_CATALOG_PATH}/${VERSION}
        ${CONTAINER_RUNTIME} cp ${CONTAINER_ID}:/manifests ${COMMUNITY_CATALOG_PATH}/${VERSION}
        ${CONTAINER_RUNTIME} cp ${CONTAINER_ID}:/metadata ${COMMUNITY_CATALOG_PATH}/${VERSION}
        ${YQ} -i '.spec.replaces = "'$(replaces ${CSV_NAME})'"' ${COMMUNITY_CATALOG_PATH}/${VERSION}/manifests/*.clusterserviceversion.yaml
        ${YQ} -i '.annotations["operators.operatorframework.io.bundle.channels.v1"] = "'$(channels ${CSV_NAME})'"' ${COMMUNITY_CATALOG_PATH}/${VERSION}/metadata/annotations.yaml
        ${CONTAINER_RUNTIME} rm ${CONTAINER_ID}
        #${CONTAINER_RUNTIME} image rm ${BUNDLE_IMAGE}
    done

    #opm validate ${CATALOG_PATH}

    echo "[DEBUG] Catalog generated"
}

main
