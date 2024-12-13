#!/bin/bash

# Operator naming
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
