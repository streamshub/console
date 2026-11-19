#!/bin/bash

set -xEeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

VERSION=""
SKIP_RANGE=""
SKOPEO_TRANSPORT="docker://"

for ARGUMENT in "$@" ; do
  KEY=$(echo "$ARGUMENT" | sed 's/=\(.*\)//')
  VALUE=$(echo "$ARGUMENT" | sed 's/^[^=]*=//')

  case "$KEY" in
    VERSION)                            VERSION=${VALUE};;
    SKIP_RANGE)                         SKIP_RANGE=${VALUE};;
    SKOPEO_TRANSPORT)                   SKOPEO_TRANSPORT=${VALUE};;
    PLATFORMS)                          PLATFORMS=${VALUE};;
    *)
  esac
done

if [ -z "${VERSION}" ] ; then
    echo "[ERROR] VERSION required"
    exit 1
fi

source ${SCRIPT_PATH}/common.sh

OPERATOR_INSTANCE_NAME="${OPERATOR_NAME}-v${VERSION}"
OPERATOR_CSV_NAME="${OPERATOR_NAME}.v${VERSION}"

BUNDLE_PATH=${SCRIPT_PATH}/../target/bundle/streamshub-console-operator
MANIFEST_PATH=${BUNDLE_PATH}/manifests
CSV_FILE_PATH=${MANIFEST_PATH}/streamshub-console-operator.clusterserviceversion.yaml

api_name="console-api"
dex_name="console-dexidp"
operator_name="console-operator"

echo "[INFO] Modify values and replace placeholders in ${CSV_FILE_PATH}"

if [[ -n "$VERSION" ]]; then
    echo "[DEBUG] Setting version = \"${VERSION}\""
    ${YQ} eval -o yaml -i ".spec.version = \"${VERSION}\"" "${CSV_FILE_PATH}"
fi

# Delete any namespaces set on the CSV deployment(s), possibly added by `quarkus.kubernetes.namespace`
${YQ} eval -o yaml -i 'del(.spec.install.spec.deployments[0].spec.template.metadata.namespace)' "${CSV_FILE_PATH}"

# Get operator image name with tag
yq_image_expression=".spec.install.spec.deployments[0].spec.template.spec.containers[0].image"
operator_image_with_tag=$(${YQ} eval "${yq_image_expression}" "${CSV_FILE_PATH}")
echo "[DEBUG] Original operator image name with tag = ${operator_image_with_tag}"

# Derive Registry and Tag for UI and API images
image_registry=$(echo "${operator_image_with_tag}" | rev | cut -d'/' -f2- | rev)
image_tag=$(echo "${operator_image_with_tag}" | sed -E -e 's/@sha256:[a-fA-F0-9]{64}//' -e 's|^([^/]+:[0-9]+/)?([^:]+):?([^/]*)|\3|' | cut -d/ -f2- | grep -v '/')
echo "[DEBUG] Image registry = ${image_registry}"
echo "[DEBUG] Image tag = ${image_tag}"

# Get operator image digest
operator_image_digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "${SKOPEO_TRANSPORT}${operator_image_with_tag}")
operator_image_with_digest="${image_registry}/${operator_name}:${image_tag}@${operator_image_digest}"

# Create relatedImages section
${YQ} eval -o yaml -i ".spec.relatedImages = null" "${CSV_FILE_PATH}"

# Replace operator image tag to digest
echo "[DEBUG] Setting container image = ${operator_image_with_digest}"
${YQ} eval -o yaml -i ".metadata.annotations.containerImage = \"${operator_image_with_digest}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.spec.containers[0].image = \"${operator_image_with_digest}\"" "${CSV_FILE_PATH}"

# Add current createdAt time
curr_time_date="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
echo "[DEBUG] Setting createdAt = ${curr_time_date}"
${YQ} eval -o yaml -i ".metadata.annotations.createdAt = \"${curr_time_date}\"" "${CSV_FILE_PATH}"

# Add the Dex CRDs to the CSV as owned by the operator.
# This avoids the requirement of granding Dex create CRD access.
INTERNAL_CRD_ARRAY=()

for dexcrd in ${MANIFEST_PATH}/*.dex.coreos.com-v1.crd.yml ; do
  INTERNAL_CRD_ARRAY+=("$(${YQ} '.metadata.name' ${dexcrd})")
  ${YQ} eval -o yaml -i '.spec.customresourcedefinitions.owned += ['"$(${YQ} '{
    "name": .metadata.name,
    "version": (.spec.versions[] | select(.served == true).name),
    "kind": .spec.names.kind,
    "description": "Internal resource used by the Dex Identity Broker. Do not use directly."
  }' "${dexcrd}" -o=json)"']' "${CSV_FILE_PATH}"
done

INTERNAL_OBJECTS="$(INTERNAL_CRDS="${INTERNAL_CRD_ARRAY[*]}" ${YQ} -n -oj 'env(INTERNAL_CRDS) | split(" ")')"

# The Dex CRDs are marked as "internal-objects" using an operator SDK annotation and will
# not display in the UI (e.g. OpenShift OperatorHub screens)
INTERNAL_OBJECTS=${INTERNAL_OBJECTS} ${YQ} -i '
  .metadata.annotations."operators.operatorframework.io/internal-objects" = strenv(INTERNAL_OBJECTS) |
  .metadata.annotations."operators.operatorframework.io/internal-objects" style="literal"' "${CSV_FILE_PATH}"

# Add an example of the Console CR and placeholders for the Dex CRs to satisfy CSV validation.
# Although internal objects, operator-sdk validation still requires examples for the Dex
# resources, so we generate stub example metadata and examples in the following sections.
EXAMPLES_META="$(${YQ} ea -oj '{
    "placeholder-" + .metadata.name: {
      "description": "Placeholder example for internal Dex resource " + .metadata.name
    }
  } as $item ireduce ({}; . * $item )' ${MANIFEST_PATH}/*.dex.coreos.com-v1.crd.yml \
  | ${YQ} -n '{
    "console-standalone-prometheus": {
      "description": "Example Console referencing a pre-existing Prometheus instance for metrics"
    }
  } * load("/dev/stdin")' | ${YQ} -oj)"

EXAMPLES_META=${EXAMPLES_META} ${YQ} -i '
  .metadata.annotations."alm-examples-metadata" = strenv(EXAMPLES_META) |
  .metadata.annotations."alm-examples-metadata" style="literal"' "${CSV_FILE_PATH}"

EXAMPLES="$(
  ${YQ} ea -oj '{
      "apiVersion": .spec.group + "/" + (.spec.versions[] | select(.served == true).name),
      "kind": .spec.names.kind,
      "metadata": {
        "name": "placeholder-" + .metadata.name
      }
    } as $item ireduce ([]; . + $item )' ${MANIFEST_PATH}/*.dex.coreos.com-v1.crd.yml | \
  ${YQ} -oj '.metadata.name = "console-standalone-prometheus" | [.] + load("/dev/stdin")' \
    ${SCRIPT_PATH}/../../examples/console/console-standalone-prometheus.yaml | ${YQ} -oj)"

EXAMPLES=${EXAMPLES} ${YQ} -i '
  .metadata.annotations."alm-examples" = strenv(EXAMPLES) |
  .metadata.annotations."alm-examples" style="literal"' "${CSV_FILE_PATH}"

# Change operator name
echo "[DEBUG] Renaming operator to ${OPERATOR_INSTANCE_NAME}"
${YQ} eval -o yaml -i ".metadata.name = \"${OPERATOR_CSV_NAME}\" | .metadata.name style=\"\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].name = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.selector.matchLabels[\"app.kubernetes.io/name\"] = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.metadata.labels[\"app.kubernetes.io/instance\"] = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"
${YQ} eval -o yaml -i ".spec.install.spec.deployments[0].spec.template.metadata.labels[\"app.kubernetes.io/name\"] = \"${OPERATOR_INSTANCE_NAME}\"" "${CSV_FILE_PATH}"

# Add Env for operator deployment that references API and Dex images with digest instead of tag
echo "[DEBUG] Add API and Dex IdP images to CSV"

api_image_with_tag=${image_registry}/${api_name}:${image_tag}
api_image_digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "${SKOPEO_TRANSPORT}${api_image_with_tag}")
api_image_with_digest="${image_registry}/${api_name}:${image_tag}@${api_image_digest}"
echo "[DEBUG] Using API image: ${api_image_with_digest}"

dex_image_with_tag=${image_registry}/${dex_name}:${image_tag}
dex_image_digest=$(${SKOPEO} inspect --tls-verify=false --override-os=linux --format "{{ .Digest }}" "${SKOPEO_TRANSPORT}${dex_image_with_tag}")
dex_image_with_digest="${image_registry}/${dex_name}:${image_tag}@${dex_image_digest}"
echo "[DEBUG] Using API image: ${dex_image_with_digest}"

${YQ} eval -o yaml -i '.spec.install.spec.deployments[0].spec.template.spec.containers[0].env += [{
  "name": "CONSOLE_DEPLOYMENT_DEX_IMAGE",
  "value": "'${dex_image_with_digest}'"
}, {
  "name": "CONSOLE_DEPLOYMENT_DEFAULT_API_IMAGE",
  "value": "'${api_image_with_digest}'"
}]' "${CSV_FILE_PATH}"

# Add operator and operand images with digests to related images
${YQ} eval -o yaml -i '.spec.relatedImages = [{
  "name": "'${OPERATOR_NAME}'",
  "image": "'${operator_image_with_digest}'"
}, {
  "name": "streamshub-console-api",
  "image": "'${api_image_with_digest}'"
}, {
  "name": "streamshub-console-dexidp",
  "image": "'${dex_image_with_digest}'"
}]' "${CSV_FILE_PATH}"

for full_arch in ${PLATFORMS//,/ } ; do
  os_arch=($(echo ${full_arch} | tr '/' '\n'))
  arch=${os_arch[1]}
  ${YQ} eval -o yaml -i ".metadata.labels[\"operatorframework.io/arch.${arch}\"] = \"supported\"" "${CSV_FILE_PATH}"
done

# Add skipRange if present
if [[ -n "$SKIP_RANGE" ]]; then
    echo "[DEBUG] Setting skipRange = \"${SKIP_RANGE}\""
    ${YQ} eval -o yaml -i ".metadata.annotations.[\"olm.skipRange\"] = \"${SKIP_RANGE}\"" "${CSV_FILE_PATH}"
fi

${YQ} -i '.spec.icon = [{ "base64data": "'$(base64 < ${SCRIPT_PATH}/../src/main/olm/icon.png | tr -d '\n')'", "mediatype": "image/png" }]' "${CSV_FILE_PATH}"

# Remove route.openshift.io from nativeAPIs - it's an optional OpenShift-only API
# and must not be a hard OLM install requirement on plain Kubernetes
echo "[DEBUG] Removing route.openshift.io from nativeAPIs"
${YQ} eval -o yaml -i 'del(.spec.nativeAPIs[] | select(.group == "route.openshift.io"))' "${CSV_FILE_PATH}"

operator-sdk bundle validate "${BUNDLE_PATH}/" --select-optional name=operatorhub
