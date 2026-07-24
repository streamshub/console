#!/usr/bin/env bash
# Installs OLM (if not already present) and deploys the Console operator's
# CatalogSource, so a Subscription created by the systemtests' OlmConfig.java
# (or by hand) has something to resolve against.
#
# Mirrors the "Deploy OLM CatalogSource" step in
# .github/workflows/systemtests.yml and the OLM install in
# systemtests/scripts/setup-minikube.sh, parameterized for local use.
#
# Cluster-agnostic — works against whatever cluster context is currently
# active (kind, minikube, or anything else), since it's pure kubectl/OLM.
#
# Usage:
#   ./setup-catalogsource.sh --image <catalog-image> [--namespace olm] [--name console-source]
#
# If you go on to run the systemtests from IntelliJ against this catalog,
# make sure to set (matching whatever --namespace/--name you used here):
#   CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE=<namespace>
#   CONSOLE_OLM_CATALOG_SOURCE_NAME=<name>
#   CONSOLE_INSTALL_TYPE=olm
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

NAMESPACE="olm"
CATALOG_NAME="console-source"
IMAGE=""

usage() {
  echo "Usage: $0 --image <catalog-image> [--namespace <ns>] [--name <catalog-source-name>]" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --namespace) NAMESPACE="$2"; shift 2 ;;
    --name) CATALOG_NAME="$2"; shift 2 ;;
    --image) IMAGE="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown argument: $1" >&2; usage ;;
  esac
done

[ -n "${IMAGE}" ] || usage

OLM_VERSION="v0.45.0"
OLM_SCRIPT_SHA256="1e8065cb503d2ee94ce82dd2591618022852f53a43df908b9f8c7d314cff3532"
OLM_INSTALL_SCRIPT=$(mktemp)
trap 'rm -f "${OLM_INSTALL_SCRIPT}"' EXIT

# True if ANY trace of a prior OLM install exists — the "olm" namespace or
# any of its CRDs. Checked broadly (not just one CRD) because a partial
# install/uninstall can leave some CRDs/namespaces behind and others gone;
# install.sh's own "already installed" guard looks at more than just
# catalogsources, so our detection needs to be at least as broad or we'll
# wrongly take the "fresh install" path into a state install.sh then
# refuses to touch, producing a confusing "already installed" error.
olm_present() {
  kubectl get namespace olm >/dev/null 2>&1 && return 0
  for crd in catalogsources clusterserviceversions installplans olmconfigs \
             operatorconditions operatorgroups operators subscriptions; do
    kubectl get crd "${crd}.operators.coreos.com" >/dev/null 2>&1 && return 0
  done
  return 1
}

# Checks a prior install exists *and* OLM's own core deployments are
# actually Available — not just present. Traces of a previous install can
# exist from one that got interrupted or crash-looped, which would make a
# presence-only check wrongly skip (re)installing, leaving a broken OLM
# that fails confusingly much later (Subscriptions never resolving,
# CatalogSources never READY).
olm_healthy() {
  olm_present || return 1
  for deploy in olm-operator catalog-operator; do
    kubectl -n olm get deployment "${deploy}" >/dev/null 2>&1 || return 1
    [ "$(kubectl -n olm get deployment "${deploy}" -o jsonpath='{.status.availableReplicas}' 2>/dev/null)" = "1" ] || return 1
  done
  return 0
}

if olm_healthy; then
  echo "OLM already installed and healthy, skipping"
else
  # The upstream install.sh refuses to touch OLM if it thinks it's already
  # installed (its own "already installed" guard) — it can't repair a
  # broken install, only create a fresh one. So if any trace of a prior
  # install exists but the health check failed, uninstall first.
  if olm_present; then
    echo "OLM CRDs are present but core deployments aren't healthy — repairing..."
    if command -v operator-sdk >/dev/null 2>&1; then
      if ! operator-sdk olm uninstall; then
        echo "Automatic OLM repair (uninstall) failed or timed out — likely because a" >&2
        echo "broken olm-operator can't clean up its own finalizers (e.g. on CSVs)." >&2
        echo "Run delete-cluster.sh (for whichever cluster you're using) to start over." >&2
        exit 1
      fi
    else
      echo "OLM appears broken (CRDs exist but olm-operator/catalog-operator aren't Available)" >&2
      echo "and 'operator-sdk' isn't available to cleanly repair it." >&2
      echo "Install it (e.g. 'brew install operator-sdk') and re-run, or run" >&2
      echo "delete-cluster.sh (for whichever cluster you're using) to start over." >&2
      exit 1
    fi
  fi

  echo "Installing OLM ${OLM_VERSION}..."
  curl --proto "=https" -o "${OLM_INSTALL_SCRIPT}" -sL \
    "https://github.com/operator-framework/operator-lifecycle-manager/releases/download/${OLM_VERSION}/install.sh"
  # Prefer sha256sum (GNU coreutils, native on Linux); fall back to shasum
  # (macOS's default, not guaranteed present on every Linux distro). Both
  # print "<hash>  <filename>", so the awk extraction is the same either way.
  if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL_SHA256=$(sha256sum "${OLM_INSTALL_SCRIPT}" | awk '{print $1}')
  else
    ACTUAL_SHA256=$(shasum -a 256 "${OLM_INSTALL_SCRIPT}" | awk '{print $1}')
  fi
  [ "${ACTUAL_SHA256}" = "${OLM_SCRIPT_SHA256}" ] \
    || { echo "OLM install script hash verification failed (got ${ACTUAL_SHA256})" >&2; exit 1; }
  chmod +x "${OLM_INSTALL_SCRIPT}"
  "${OLM_INSTALL_SCRIPT}" "${OLM_VERSION}"

  echo "Waiting for OLM core deployments to become available..."
  kubectl -n olm wait --for=condition=available deployment/olm-operator --timeout=180s
  kubectl -n olm wait --for=condition=available deployment/catalog-operator --timeout=180s

  olm_healthy || { echo "OLM install completed but health check still fails" >&2; exit 1; }
fi

kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

echo "Deploying CatalogSource '${CATALOG_NAME}' in namespace '${NAMESPACE}' (image=${IMAGE})..."
yq ea ".metadata.name = \"${CATALOG_NAME}\" | .spec.image = \"${IMAGE}\"" \
  ../../../install/operator/olm/010-CatalogSource-console-operator-catalog.yaml \
  | kubectl apply -n "${NAMESPACE}" -f -

echo "Waiting for CatalogSource to become READY..."
kubectl wait "catalogsource/${CATALOG_NAME}" -n "${NAMESPACE}" \
  --for=jsonpath='{.status.connectionState.lastObservedState}'=READY \
  --timeout=180s

echo ""
echo "CatalogSource ready: ${CATALOG_NAME} (namespace: ${NAMESPACE}, image: ${IMAGE})"
echo "For systemtests via IntelliJ, set:"
echo "  CONSOLE_INSTALL_TYPE=olm"
echo "  CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE=${NAMESPACE}"
echo "  CONSOLE_OLM_CATALOG_SOURCE_NAME=${CATALOG_NAME}"
