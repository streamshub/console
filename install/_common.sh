RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color
INFO="[ \033[38;5;33mINFO${NC} ]"
WARN="[ \033[38;5;208mWARN${NC} ]"
ERROR="[ \033[38;5;196mERROR${NC} ]"

KUBE="$(which oc 2>/dev/null || which kubectl 2>/dev/null)" || :

if [ "${KUBE}" == "" ] ; then
    echo -e "${ERROR} Neither 'oc' or 'kubectl' command line utilities found on the PATH"
    exit 1
fi

YQ="$(which yq 2>/dev/null)" || :

if [ "${YQ}" == "" ] ; then
    echo -e "${ERROR} 'yq' command line utility found on the PATH"
    exit 1
fi

if ${KUBE} get namespace/${NAMESPACE} >/dev/null 2>&1 ; then
    echo -e "${INFO} Namespace '${NAMESPACE}' exists"
else
    echo -e "${WARN} Namespace '${NAMESPACE}' not found... creating"
    ${KUBE} create namespace ${NAMESPACE} >/dev/null

    if ${KUBE} get namespace/${NAMESPACE} >/dev/null 2>&1 ; then
        echo -e "${INFO} Namespace '${NAMESPACE}' created"
    else
        echo -e "${WARN} Namespace '${NAMESPACE}' could not be created"
    fi
fi

OLM=$(kubectl get crd | grep operators.coreos.com) || :

if [ "${OLM}" == "" ] ; then
    echo -e "${ERROR} Operator Lifecycle Manager not found, please install it.

$ operator-sdk olm install

For more info please visit https://sdk.operatorframework.io/
"
    exit 1
fi

function fetch_available_packages {
    local NAME_PATTERN="${1}"

    for pm in $(${KUBE} get packagemanifests -o name | grep -E '^packagemanifest\.packages\.operators\.coreos\.com/('"${NAME_PATTERN}"')$') ; do
        ${KUBE} get $pm -o yaml | ${YQ} -o=json '{
            "name": .status.packageName,
            "channel": .status.defaultChannel,
            "catalogSource": .status.catalogSource,
            "catalogSourceNamespace": .status.catalogSourceNamespace
        }'
    done | ${YQ} ea -p=json '[.]' | ${YQ} -o=csv | tail -n +2
}

function display_suggested_subscription {
    local OPERATOR_NAME="${1}"
    local NAME_PATTERN="${2}"

    local AVAILABLE_PKGS="$(fetch_available_packages "${NAME_PATTERN}")"
    echo -e "${INFO} ${OPERATOR_NAME} may be installed by creating one of the following resources:"
    COUNTER=0

    while IFS=, read -r PKG_NAME PKG_CHANNEL PKG_CTLG_SRC PKG_CTLG_SRC_NS; do
        COUNTER=$(( COUNTER + 1 ))
        echo -e "${INFO} ----- Option ${COUNTER} -----"
        echo "apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: ${OPERATOR_NAME}
  namespace: ${NAMESPACE}
spec:
  name: ${PKG_NAME}
  channel: ${PKG_CHANNEL}
  source: ${PKG_CTLG_SRC}
  sourceNamespace: ${PKG_CTLG_SRC_NS}" | ${YQ}
    done < <(echo "${AVAILABLE_PKGS}")
}
