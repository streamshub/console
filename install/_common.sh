RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color
INFO="[ \033[38;5;33mINFO${NC} ]"
WARN="[ \033[38;5;208mWARN${NC} ]"
ERROR="[ \033[38;5;196mERROR${NC} ]"

function fetch_available_packages {
    local NAME_PATTERN="${1}"

    for pm in $(kubectl get packagemanifests -o name | grep -Pe '^packagemanifest\.packages\.operators\.coreos\.com/('"${NAME_PATTERN}"')$') ; do
        kubectl get $pm -o yaml | yq -o=json '{
            "name": .status.packageName,
            "channel": .status.defaultChannel,
            "catalogSource": .status.catalogSource,
            "catalogSourceNamespace": .status.catalogSourceNamespace
        }'
    done | yq ea -p=json '[.]' | yq -o=csv | tail -n +2
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
  sourceNamespace: ${PKG_CTLG_SRC_NS}" | yq
    done < <(echo "${AVAILABLE_PKGS}")
}

if kubectl get namespace/${NAMESPACE} >/dev/null 2>&1 ; then
    echo -e "${INFO} Namespace '${NAMESPACE}' exists"
else
    echo -e "${WARN} Namespace '${NAMESPACE}' not found... creating"
    kubectl create namespace ${NAMESPACE} >/dev/null

    if kubectl get namespace/${NAMESPACE} >/dev/null 2>&1 ; then
        echo -e "${INFO} Namespace '${NAMESPACE}' created"
    else
        echo -e "${WARN} Namespace '${NAMESPACE}' could not be created"
    fi
fi
