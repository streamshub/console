# System Tests Scripts

Utility scripts for setting up test infrastructure.

## OLM Catalog Setup

### setup-olm-catalog.sh

Creates an OLM CatalogSource for the Console Operator.

**Requirements:**
- `kubectl` (configured and connected to cluster)
- `envsubst` (from gettext package)
  - macOS: `brew install gettext`
  - Linux: `apt-get install gettext-base`

**Usage:**

```bash
./setup-olm-catalog.sh --index-image <image> [OPTIONS]
```

**Arguments:**

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `--index-image` | Yes | - | OLM index/catalog image |
| `--name` | No | `console-source` | CatalogSource name |
| `--namespace` | No | `openshift-marketplace` | CatalogSource namespace |

**Examples:**

```bash
# Basic usage with index image
./setup-olm-catalog.sh --index-image quay.io/streamshub/console-index:latest

# Custom catalog name and namespace
./setup-olm-catalog.sh \
  --index-image quay.io/streamshub/console-index:0.12.4 \
  --name my-catalog \
  --namespace my-namespace
```

**What it does:**

1. Validates required commands are available
2. Creates namespace if it doesn't exist
3. Deletes existing CatalogSource (if present)
4. Creates CatalogSource from template (`catalog/catalogsource.yaml`)
5. Waits for catalog pod to be ready
6. Provides verification commands

**Template:**

The script uses `catalog/catalogsource.yaml` as a template with variable substitution:
- `${CATALOG_NAME}` - CatalogSource name
- `${CATALOG_NAMESPACE}` - CatalogSource namespace  
- `${INDEX_IMAGE}` - OLM index image

**After setup:**

Update `systemtests/config.yaml`:
```yaml
CONSOLE_INSTALL_TYPE: olm
CONSOLE_OLM_CATALOG_SOURCE_NAME: console-source  # Match your catalog name
CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE: openshift-marketplace
CONSOLE_OLM_PACKAGE_NAME: streamshub-console-operator
```

Then run tests:
```bash
mvn -pl systemtests verify
```

**Verification:**

```bash
# Check CatalogSource
kubectl get catalogsource console-source -n openshift-marketplace

# Check catalog pod
kubectl get pods -n openshift-marketplace | grep console-source

# List available operators
kubectl get packagemanifests | grep console
```

## Directory Structure

```
scripts/
├── README.md                    # This file
├── setup-olm-catalog.sh         # OLM catalog setup script
└── catalog/
    └── catalogsource.yaml       # CatalogSource template
```
