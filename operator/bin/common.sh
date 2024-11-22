#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

BUNDLE_PATH=${SCRIPT_PATH}/../target/bundle/console-operator/
CSV_FILE_PATH=${BUNDLE_PATH}/manifests/console-operator.clusterserviceversion.yaml
CATALOG_PATH=${SCRIPT_PATH}/../target/catalog
# Operator naming
ORIGINAL_OPERATOR_NAME="console-operator"
OPERATOR_NAME="streamshub-console-operator"
OPERATOR_INSTANCE_NAME="${OPERATOR_NAME}-v${VERSION}"
OPERATOR_CSV_NAME="${OPERATOR_NAME}.v${VERSION}"

YQ="$(which yq 2>/dev/null)" || :

if [ "${YQ}" == "" ] ; then
    echo -e "'yq' is not installed, please visit https://github.com/mikefarah/yq for more info"
    exit 1
fi

SKOPEO="$(which skopeo 2>/dev/null)" || :

if [ "${SKOPEO}" == "" ] ; then
    echo "'skopeo' is not installed, please visit https://github.com/containers/skopeo/blob/main/install.md for more info"
    exit 1
fi
