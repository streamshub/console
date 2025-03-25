#!/bin/bash

set -Eeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

OPERATOR_SDK_VERSION=${OPERATOR_SDK_VERSION:-v1.39.2}
OPM_VERSION=${OPM_VERSION:-v1.51.1}

curl -L -o operator-sdk https://github.com/operator-framework/operator-sdk/releases/download/${OPERATOR_SDK_VERSION}/operator-sdk_linux-amd64
chmod +x operator-sdk
sudo cp -v operator-sdk /usr/bin/
rm -vf operator-sdk

curl -L -o opm https://github.com/operator-framework/operator-registry/releases/download/${OPM_VERSION}/linux-amd64-opm
chmod +x opm
sudo cp -v opm /usr/bin/
rm -vf opm
