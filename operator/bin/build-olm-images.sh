#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

export CONTAINER_RUNTIME="${1?Please provide the container runtime command}"
export CONSOLE_OPERATOR_BUNDLE_IMAGE="${2}"
export CONSOLE_OPERATOR_CATALOG_IMAGE="${3}"
export VERSION="${4}"

${CONTAINER_RUNTIME} build -t ${CONSOLE_OPERATOR_BUNDLE_IMAGE} -f ${SCRIPT_PATH}/../target/bundle/console-operator/bundle.Dockerfile

CATALOG_PATH=${SCRIPT_PATH}/../target/catalog
rm -rvf ${CATALOG_PATH} ${CATALOG_PATH}.Dockerfile
mkdir -p ${CATALOG_PATH}

opm generate dockerfile ${CATALOG_PATH}
opm init console-operator --default-channel=alpha --output=yaml > ${CATALOG_PATH}/operator.yaml
opm render operator/target/bundle/console-operator/ --output=yaml >> ${CATALOG_PATH}/operator.yaml
cat << EOF >> ${CATALOG_PATH}/operator.yaml
---
schema: olm.channel
package: console-operator
name: alpha
entries:
  - name: console-operator.v${VERSION}
EOF

opm validate ${CATALOG_PATH}

${CONTAINER_RUNTIME} build -t ${CONSOLE_OPERATOR_CATALOG_IMAGE} -f ${CATALOG_PATH}.Dockerfile
