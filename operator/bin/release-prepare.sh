#!/bin/bash

set -xEeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
OPERATOR_PATH="$(cd -- "${SCRIPT_PATH}/.." >/dev/null 2>&1 ; pwd -P)"

PRERELEASE_VERSION="${1?prerelease version is required}"
RELEASE_VERSION="${2?release version is required}"
POSTRELEASE_VERSION="${3?postrelease version is required}"
VERSION="${4?current version is required}"
IMAGE_NAME="${5?image name is required}"
GIT_STAGE="${6:-false}"

source ${SCRIPT_PATH}/common.sh

PRERELEASE_CSV_NAME="${OPERATOR_NAME}.v${PRERELEASE_VERSION}"
RELEASE_CSV_NAME="${OPERATOR_NAME}.v${RELEASE_VERSION}"
POSTRELEASE_CSV_NAME="${OPERATOR_NAME}.v${POSTRELEASE_VERSION}"

if [ "${VERSION}" == "${POSTRELEASE_VERSION}" ] ; then
    # Running via the `completionGoals` of the maven-release-plugin
    echo "[INFO] Running release completion with version ${VERSION}"

    # Create new entry for the next development cycle, "replaces" the version just released
    ${YQ} -i '.entries += {
        "name": "'${POSTRELEASE_CSV_NAME}'",
        "replaces": "'${RELEASE_CSV_NAME}'"
    }' ${OPERATOR_PATH}/src/main/olm/channel.alpha.yaml

    if [ "${GIT_STAGE}" == "true" ] ; then
        git add ${OPERATOR_PATH}/src/main/olm/channel.alpha.yaml
    fi
else
    # Running via the `preparationGoals` of the maven-release-plugin
    echo "[INFO] Running release preparation for version ${VERSION}"

    # Change the name of the entry to be the released version (e.g removing the -snapshot suffix)
    ${YQ} -i '(.entries[].name | select(. == "'${PRERELEASE_CSV_NAME}'")) = "'${RELEASE_CSV_NAME}'"' \
      ${OPERATOR_PATH}/src/main/olm/channel.alpha.yaml

    # Create a file with a reference to the bundle image to be generated during the release
    echo "image: ${IMAGE_NAME}:${VERSION}" > ${OPERATOR_PATH}/src/main/olm/bundles/${RELEASE_CSV_NAME}.yaml

    if [ "${GIT_STAGE}" == "true" ] ; then
        git add ${OPERATOR_PATH}/src/main/olm/channel.alpha.yaml ${OPERATOR_PATH}/src/main/olm/bundles/${RELEASE_CSV_NAME}.yaml
    fi
fi
