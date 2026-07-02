#!/usr/bin/env bash

# Setup OLM CatalogSource for Console Operator
# This script creates a CatalogSource to enable OLM-based operator installation
#
# Requirements:
#   - kubectl (configured and connected to cluster)
#   - envsubst (from gettext package)

set -euo pipefail

# Check required commands
for cmd in kubectl envsubst; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ERROR: Required command '$cmd' not found"
        if [[ "$cmd" == "envsubst" ]]; then
            echo "Install: brew install gettext (macOS) or apt-get install gettext-base (Linux)"
        fi
        exit 1
    fi
done

# Default values
CATALOG_NAME="${CATALOG_NAME:-console-source}"
CATALOG_NAMESPACE="${CATALOG_NAMESPACE:-openshift-marketplace}"
INDEX_IMAGE="${INDEX_IMAGE:-}"

# Print usage
usage() {
    cat <<EOF
Usage: $0 --index-image <image> [OPTIONS]

Setup OLM CatalogSource for Console Operator

Required Arguments:
  --index-image <image>     OLM index/catalog image (e.g., quay.io/streamshub/console-index:latest)

Optional Arguments:
  --name <name>             CatalogSource name (default: console-source)
  --namespace <namespace>   CatalogSource namespace (default: openshift-marketplace)
  -h, --help                Show this help message

Examples:
  # Setup catalog with custom index image
  $0 --index-image quay.io/streamshub/console-index:0.12.4

  # Setup with custom name and namespace
  $0 --index-image quay.io/myrepo/catalog:latest --name my-catalog --namespace my-namespace

EOF
    exit 1
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --index-image)
            INDEX_IMAGE="$2"
            shift 2
            ;;
        --name)
            CATALOG_NAME="$2"
            shift 2
            ;;
        --namespace)
            CATALOG_NAMESPACE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "ERROR: Unknown argument: $1"
            usage
            ;;
    esac
done

# Validate required arguments
if [[ -z "$INDEX_IMAGE" ]]; then
    echo "ERROR: --index-image is required"
    usage
fi

echo "========================================="
echo "OLM CatalogSource Setup"
echo "========================================="
echo "Catalog Name:      $CATALOG_NAME"
echo "Namespace:         $CATALOG_NAMESPACE"
echo "Index Image:       $INDEX_IMAGE"
echo "========================================="

# Check if namespace exists, create if not
if ! kubectl get namespace "$CATALOG_NAMESPACE" &>/dev/null; then
    echo "Creating namespace: $CATALOG_NAMESPACE"
    kubectl create namespace "$CATALOG_NAMESPACE"
else
    echo "Namespace $CATALOG_NAMESPACE already exists"
fi

# Check if CatalogSource already exists
if kubectl get catalogsource "$CATALOG_NAME" -n "$CATALOG_NAMESPACE" &>/dev/null; then
    echo "CatalogSource $CATALOG_NAME already exists in namespace $CATALOG_NAMESPACE"
    echo "Deleting existing CatalogSource..."
    kubectl delete catalogsource "$CATALOG_NAME" -n "$CATALOG_NAMESPACE"
    sleep 2
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CATALOG_TEMPLATE="$SCRIPT_DIR/catalog/catalogsource.yaml"

# Check if template exists
if [[ ! -f "$CATALOG_TEMPLATE" ]]; then
    echo "ERROR: CatalogSource template not found at: $CATALOG_TEMPLATE"
    exit 1
fi

# Create CatalogSource from template
echo "Creating CatalogSource from template..."
export CATALOG_NAME CATALOG_NAMESPACE INDEX_IMAGE
envsubst < "$CATALOG_TEMPLATE" | kubectl apply -f -

echo ""
echo "Waiting for CatalogSource to be ready..."
kubectl wait catalogsource/"$CATALOG_NAME" \
    -n "$CATALOG_NAMESPACE" \
    --for=jsonpath='{.status.connectionState.lastObservedState}'=READY \
    --timeout=5m

echo ""
echo "=== CatalogSource ready ==="
