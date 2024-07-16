#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
source ${SCRIPT_PATH}/common.sh

BUNDLE_PATH=${SCRIPT_PATH}/../target/bundle/console-operator/
CSV_FILE_PATH=${BUNDLE_PATH}/manifests/console-operator.clusterserviceversion.yaml
CATALOG_PATH=${SCRIPT_PATH}/../target/catalog

ARCH=linux/amd64
VERSION="latest"
CONSOLE_OPERATOR_IMAGE="quay.io/streamshub/console-operator:${VERSION}"
SKIP_RANGE=""

for ARGUMENT in "$@"
do
  KEY=$(echo "$ARGUMENT" | sed 's/=\(.*\)//')
  VALUE=$(echo "$ARGUMENT" | sed 's/^[^=]*=//')

  case "$KEY" in
    ARCH)     			                        ARCH=${VALUE} ;;
    CONSOLE_OPERATOR_IMAGE)	  CONSOLE_OPERATOR_IMAGE=${VALUE} ;;
    SKIP_RANGE)                            SKIP_RANGE=${VALUE};;
    VERSION)                                  VERSION=${VALUE};;
    *)
  esac
done

function replace_placeholders() {
    echo "[INFO] Replacing placeholders"
    yq_image_expression=".spec.install.spec.deployments[0] | (select (.name ==\"console-operator\")).spec.template.spec.containers[].image"

    full_image=$(${YQ} eval "${yq_image_expression}" "${CSV_FILE_PATH}")
    echo "[DEBUG] Image original name = ${full_image}"

    image_name=$(echo "${full_image}" | cut -d':' -f1)
    digest=$(${SKOPEO} inspect --override-arch ${ARCH} --format "{{ .Digest }}" "docker://${full_image}")
    image_with_digest="${image_name}@${digest}"

    echo "[DEBUG] Setting container image = ${image_with_digest}"
    ${YQ} eval -o yaml -i ".metadata.annotations.containerImage = \"${image_with_digest}\"" "${CSV_FILE_PATH}"

    curr_time_date=$(date +'%Y-%m-%d %H:%M:%S')
    echo "[DEBUG] Setting createdAt = ${curr_time_date}"
    ${YQ} eval -o yaml -i ".metadata.annotations.createdAt = \"${curr_time_date}\"" "${CSV_FILE_PATH}"

    if [[ -n "$SKIP_RANGE" ]]; then
        echo "[DEBUG] Setting skipRange = \"${SKIP_RANGE}\""
        ${YQ} eval -o yaml -i ".metadata.annotations.[\"olm.skipRange\"] = \"${SKIP_RANGE}\"" "${CSV_FILE_PATH}"
    fi
}

function build_catalog_image() {
    echo "[INFO] Build catalog image"
    rm -rvf ${CATALOG_PATH} ${CATALOG_PATH}.Dockerfile
    mkdir -p ${CATALOG_PATH}

    opm generate dockerfile ${CATALOG_PATH}
    opm init console-operator --default-channel=alpha --output=yaml > ${CATALOG_PATH}/operator.yaml
    opm render ${BUNDLE_PATH} --output=yaml >> ${CATALOG_PATH}/operator.yaml

cat << EOF >> ${CATALOG_PATH}/operator.yaml
---
schema: olm.channel
package: console-operator
name: alpha
entries:
  - name: console-operator.v${VERSION}
EOF
}

replace_placeholders
build_catalog_image
opm validate ${CATALOG_PATH}