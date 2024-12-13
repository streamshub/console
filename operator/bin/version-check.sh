#!/bin/bash

set -xEeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
OPERATOR_PATH="$(cd -- "${SCRIPT_PATH}/.." >/dev/null 2>&1 ; pwd -P)"

VERSION="${1?version is required}"

source ${SCRIPT_PATH}/common.sh
ALPHA_CHANNEL=${OPERATOR_PATH}/src/main/olm/channel.alpha.yaml

if [ "$(${YQ} '(.entries[].name | select(. == "'${OPERATOR_CSV_NAME}'"))' ${ALPHA_CHANNEL})" != "" ] ; then
    echo "[INFO] Bundle ${OPERATOR_CSV_NAME} has an entry in ${ALPHA_CHANNEL}"
    exit 0;
fi

echo "[ERROR] Bundle ${OPERATOR_CSV_NAME} has no entry in ${ALPHA_CHANNEL}"
exit 1;
