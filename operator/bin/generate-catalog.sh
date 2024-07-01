#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

export VERSION="${1}"

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
