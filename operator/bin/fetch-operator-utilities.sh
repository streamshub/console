#!/bin/bash

set -Eeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

OPERATOR_SDK_VERSION="${OPERATOR_SDK_VERSION:-v1.39.2}"
OPM_VERSION="${OPM_VERSION:-v1.51.0}"

case "$(uname -s)" in
  Linux)  OS=linux;  INSTALL_DIR=/usr/bin ;;
  Darwin) OS=darwin; INSTALL_DIR=/usr/local/bin ;; # /usr/bin is protected by SIP on macOS
  *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac

case "$(uname -m)" in
  x86_64|amd64)  ARCH=amd64 ;;
  arm64|aarch64) ARCH=arm64 ;;
  *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

sudo mkdir -p "${INSTALL_DIR}"

curl -L -s -o operator-sdk https://github.com/operator-framework/operator-sdk/releases/download/${OPERATOR_SDK_VERSION}/operator-sdk_${OS}_${ARCH}
chmod +x operator-sdk
sudo cp -v operator-sdk "${INSTALL_DIR}/"
rm -vf operator-sdk

curl -L -s -o opm https://github.com/operator-framework/operator-registry/releases/download/${OPM_VERSION}/${OS}-${ARCH}-opm
chmod +x opm
sudo cp -v opm "${INSTALL_DIR}/"
rm -vf opm
