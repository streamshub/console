#!/bin/bash

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"
source ${SCRIPT_PATH}/common.sh

SKIP_RANGE=""

for ARGUMENT in "$@"
do
  KEY=$(echo "$ARGUMENT" | sed 's/=\(.*\)//')
  VALUE=$(echo "$ARGUMENT" | sed 's/^[^=]*=//')

  case "$KEY" in
    SKIP_RANGE)                            SKIP_RANGE=${VALUE};;
    ORIGINAL_OPERATOR_NAME)                      ORIGINAL_OPERATOR_NAME=${VALUE};;
    *)
  esac
done

echo "[INFO] Modify values and replace placeholders in ${CSV_FILE_PATH}"

# Delete any namespaces set on the CSV deployment(s), possibly added by `quarkus.kubernetes.namespace`
${YQ} eval -o yaml -i 'del(.spec.install.spec.deployments[0].spec.template.metadata.namespace)' "${CSV_FILE_PATH}"

yq_image_expression=".spec.install.spec.deployments[0] | (select (.name ==\"${ORIGINAL_OPERATOR_NAME}\")).spec.template.spec.containers[].image"

full_image=$(${YQ} eval "${yq_image_expression}" "${CSV_FILE_PATH}")
echo "[DEBUG] Original image name = ${full_image}"

image_name=$(echo "${full_image}" | rev | cut -d':' -f2- | rev)
digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "docker://${full_image}")
image_with_digest="${image_name}@${digest}"

# Create relatedImages section
${YQ} eval -o yaml -i ".spec.relatedImages = null" "${CSV_FILE_PATH}"

# Add operator image
${YQ} eval -o yaml -i ".spec.relatedImages += [{\"name\": \"${OPERATOR_NAME}\", \"image\": \"${image_with_digest}\"}]" "${CSV_FILE_PATH}";
echo "[DEBUG] Setting container image = ${image_with_digest}"
${YQ} eval -o yaml -i ".metadata.annotations.containerImage = \"${image_with_digest}\"" "${CSV_FILE_PATH}"

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

# Add skipRange if present
if [[ -n "$SKIP_RANGE" ]]; then
    echo "[DEBUG] Setting skipRange = \"${SKIP_RANGE}\""
    ${YQ} eval -o yaml -i ".metadata.annotations.[\"olm.skipRange\"] = \"${SKIP_RANGE}\"" "${CSV_FILE_PATH}"
fi
