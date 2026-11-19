#!/bin/bash

set -Eeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
OPERATOR_PATH="$(cd -- "${SCRIPT_PATH}/.." >/dev/null 2>&1 ; pwd -P)"

DEX_CRDS_PATH="${OPERATOR_PATH}/target/dex-crds"
KUBERNETES_PATH="${OPERATOR_PATH}/target/kubernetes"

for crd in "${DEX_CRDS_PATH}"/*.yaml "${KUBERNETES_PATH}"/consoles.console.streamshub.github.com-v1.yml ; do
    echo "---"
    cat "$crd"
done

cat "${KUBERNETES_PATH}"/kubernetes.yml
