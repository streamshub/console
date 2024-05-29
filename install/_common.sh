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
