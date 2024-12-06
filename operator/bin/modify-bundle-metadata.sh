#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

VERSION=""
SKIP_RANGE=""
SKOPEO_TRANSPORT="docker://"

for ARGUMENT in "$@"
do
  KEY=$(echo "$ARGUMENT" | sed 's/=\(.*\)//')
  VALUE=$(echo "$ARGUMENT" | sed 's/^[^=]*=//')

  case "$KEY" in
    VERSION)                            VERSION=${VALUE};;
    SKIP_RANGE)                         SKIP_RANGE=${VALUE};;
    ORIGINAL_OPERATOR_NAME)             ORIGINAL_OPERATOR_NAME=${VALUE};;
    SKOPEO_TRANSPORT)                   SKOPEO_TRANSPORT=${VALUE};;
    *)
  esac
done

source ${SCRIPT_PATH}/common.sh

api_name="console-api"
ui_name="console-ui"
operator_name="console-operator"

echo "[INFO] Modify values and replace placeholders in ${CSV_FILE_PATH}"

if [[ -n "$VERSION" ]]; then
    echo "[DEBUG] Setting version = \"${VERSION}\""
    ${YQ} eval -o yaml -i ".spec.version = \"${VERSION}\"" "${CSV_FILE_PATH}"
fi

# Delete any namespaces set on the CSV deployment(s), possibly added by `quarkus.kubernetes.namespace`
${YQ} eval -o yaml -i 'del(.spec.install.spec.deployments[0].spec.template.metadata.namespace)' "${CSV_FILE_PATH}"

# Get operator image name with tag
yq_image_expression=".spec.install.spec.deployments[0] | (select (.name ==\"${ORIGINAL_OPERATOR_NAME}\")).spec.template.spec.containers[].image"
operator_image_with_tag=$(${YQ} eval "${yq_image_expression}" "${CSV_FILE_PATH}")
echo "[DEBUG] Original operator image name with tag = ${operator_image_with_tag}"

# Derive Registry and Tag for UI and API images
image_registry=$(echo "${operator_image_with_tag}" | rev | cut -d'/' -f2- | rev)
image_tag=$(echo "${operator_image_with_tag}" | rev | cut -d':' -f1 | rev)
echo "[DEBUG] Image registry = ${image_registry}"
echo "[DEBUG] Image tag = ${image_tag}"

# Get operator image digest
operator_image_digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "${SKOPEO_TRANSPORT}${operator_image_with_tag}")
operator_image_with_digest="${image_registry}/${operator_name}@${operator_image_digest}"

# Create relatedImages section
${YQ} eval -o yaml -i ".spec.relatedImages = null" "${CSV_FILE_PATH}"

# Add operator image with digest to related images + replace operator image tag to digest
echo "[DEBUG] Setting container image = ${operator_image_with_digest}"
${YQ} eval -o yaml -i ".spec.relatedImages += [{\"name\": \"${OPERATOR_NAME}\", \"image\": \"${operator_image_with_digest}\"}]" "${CSV_FILE_PATH}";
${YQ} eval -o yaml -i ".metadata.annotations.containerImage = \"${operator_image_with_digest}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.spec.containers[0].image = \"${operator_image_with_digest}\"" "${CSV_FILE_PATH}"

# Add current createdAt time
curr_time_date="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
echo "[DEBUG] Setting createdAt = ${curr_time_date}"
${YQ} eval -o yaml -i ".metadata.annotations.createdAt = \"${curr_time_date}\"" "${CSV_FILE_PATH}"

# Change operator name
echo "[DEBUG] Renaming operator to ${OPERATOR_INSTANCE_NAME}"
${YQ} eval -o yaml -i ".metadata.name = \"${OPERATOR_CSV_NAME}\" | .metadata.name style=\"\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].name = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.selector.matchLabels[\"app.kubernetes.io/name\"] = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.metadata.labels[\"app.kubernetes.io/instance\"] = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.metadata.labels[\"app.kubernetes.io/name\"] = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.spec.containers[0].name = \"${OPERATOR_NAME}\"" "${CSV_FILE_PATH}"
# Change serviceAccountName as well
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.spec.serviceAccountName = \"${OPERATOR_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.clusterPermissions.[].serviceAccountName = \"${OPERATOR_NAME}\"" "${CSV_FILE_PATH}"

echo "[DEBUG] Updating package name annotation and image label to ${OPERATOR_NAME}"
${YQ} eval -o yaml -i ".annotations.[\"operators.operatorframework.io.bundle.package.v1\"] = \"${OPERATOR_NAME}\"" "${BUNDLE_PATH}/metadata/annotations.yaml"
sed -i 's/'${ORIGINAL_OPERATOR_NAME}'/'${OPERATOR_NAME}'/' "${BUNDLE_PATH}/bundle.Dockerfile"

# Add Env for operator deployment that references API and UI images with digest instead of tag
echo "[DEBUG] Add UI and API images to CSV"

ui_image_with_tag=${image_registry}/${ui_name}:${image_tag}
ui_image_digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "${SKOPEO_TRANSPORT}${ui_image_with_tag}")
ui_image_with_digest="${image_registry}/${ui_name}@${ui_image_digest}"
echo "[DEBUG] Using UI image: ${ui_image_with_digest}"

api_image_with_tag=${image_registry}/${api_name}:${image_tag}
api_image_digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "${SKOPEO_TRANSPORT}${api_image_with_tag}")
api_image_with_digest="${image_registry}/${api_name}@${api_image_digest}"
echo "[DEBUG] Using API image: ${api_image_with_digest}"

${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.spec.containers[0].env += [{\"name\": \"CONSOLE_DEPLOYMENT_DEFAULT_UI_IMAGE\", \"value\": \"${ui_image_with_digest}\"}]" "${CSV_FILE_PATH}";
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.spec.containers[0].env += [{\"name\": \"CONSOLE_DEPLOYMENT_DEFAULT_API_IMAGE\", \"value\": \"${api_image_with_digest}\"}]" "${CSV_FILE_PATH}";

# Add skipRange if present
if [[ -n "$SKIP_RANGE" ]]; then
    echo "[DEBUG] Setting skipRange = \"${SKIP_RANGE}\""
    ${YQ} eval -o yaml -i ".metadata.annotations.[\"olm.skipRange\"] = \"${SKIP_RANGE}\"" "${CSV_FILE_PATH}"
fi
