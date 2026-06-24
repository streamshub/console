# System Tests Configuration Reference

This document details all configuration options available in `systemtests/config.yaml`.

## Table of Contents

- [Configuration File Location](#configuration-file-location)
- [How to Override Configuration](#how-to-override-configuration)
- [Configuration Categories](#configuration-categories)
  - [Common Settings](#common-settings)
  - [OLM Configuration](#olm-configuration)
  - [YAML Installation Configuration](#yaml-installation-configuration)
  - [Console Instance Images](#console-instance-images)
  - [Component Versions](#component-versions)
- [Configuration Examples](#configuration-examples)

## Configuration File Location

Primary configuration: [`systemtests/config.yaml`](config.yaml)

This file contains all configuration variables that control test behavior, deployment methods, component versions, and more.

## How to Override Configuration

Two methods for customizing configuration:

**1. Edit config.yaml directly** (persistent changes)
```yaml
# Edit systemtests/config.yaml
RUN_HEADLESS: false
TEST_CONSOLE_LOG_LEVEL: DEBUG
```

**2. Set environment variables** (temporary overrides)
```bash
export RUN_HEADLESS=false
export TEST_CONSOLE_LOG_LEVEL=DEBUG
mvn -pl systemtests verify
```

Environment variables take precedence over config.yaml values.

## Configuration Categories

### Common Settings

General test behavior and logging configuration:

| Variable | Default | Description |
|----------|---------|-------------|
| `CLEANUP_ENVIRONMENT` | `true` | Clean up Kubernetes resources after each test. Set to `false` to inspect cluster state after test failures. |
| `DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL` | `true` | Re-deploy Console operator if it already exists in the cluster. |
| `TEST_FILE_LOG_LEVEL` | `DEBUG` | Log level for file output. Options: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`. |
| `TEST_CONSOLE_LOG_LEVEL` | `INFO` | Log level for console output. Keeps console clean while files have details. |
| `TEST_LOG_DIR` | `target/logs` | Directory path for test logs, YAML dumps, and diagnostic output. |
| `RUN_HEADLESS` | `true` | Run Playwright tests without visible browser window. Set to `false` to watch UI tests execute. |
| `SCREENSHOTS_DIR_PATH` | `screenshots` | Directory for test failure screenshots. Relative to systemtests directory. |
| `SKIP_STRIMZI_INSTALLATION` | `false` | Skip Strimzi installation if already installed externally. |
| `STRIMZI_OPERATOR_NAME` | `strimzi-cluster-operator` | Deployment name of Strimzi Cluster Operator. Update if using external Strimzi. |
| `CONSOLE_DEPLOYMENT_NAME` | `streamshub-console` | Deployment name of Console instance. |
| `CONSOLE_INSTALL_TYPE` | `olm` | Installation method: `olm` (Operator Lifecycle Manager) or `yaml` (direct manifests). |
| `CLIENT_TYPE` | `kubectl` | Kubernetes CLI client: `kubectl` or `oc` (for OpenShift). |
| `CONSOLE_CLUSTER_DOMAIN` | `192.168.49.2.nip.io` | Cluster domain for ingress routes. Required for non-OpenShift clusters. Update to match your cluster IP. |

### OLM Configuration

Settings for Operator Lifecycle Manager installation method:

| Variable | Default | Description |
|----------|---------|-------------|
| `CONSOLE_OLM_CATALOG_SOURCE_NAME` | `console-source` | Name of the CatalogSource for Console Operator. |
| `CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE` | `openshift-marketplace` | Namespace where CatalogSource is created. |
| `CONSOLE_OLM_PACKAGE_NAME` | `streamshub-console-operator` | Package name in the operator catalog. |
| `CONSOLE_OLM_CHANNEL_NAME` | `alpha` | Subscription channel for selecting operator version. |

**When to use OLM:** Testing on OpenShift or when validating operator bundle/catalog functionality.

### YAML Installation Configuration

Settings for direct YAML manifest installation:

| Variable | Default | Description |
|----------|---------|-------------|
| `STRIMZI_OPERATOR_VERSION` | `1.0.0` | Version of Strimzi Cluster Operator to install. |
| `CONSOLE_OPERATOR_BUNDLE_URL` | `https://github.com/streamshub/console/releases/download/0.12.4/streamshub-console-operator.yaml` | URL or path to console-operator.yaml. Accepts: URL, relative path from project root, or absolute path. |

**When to use YAML:** Testing on non-OpenShift clusters (minikube, kind, k3s) or when testing specific operator builds.

### Console Instance Images

Override default images installed by the operator:

| Variable | Default | Description |
|----------|---------|-------------|
| `CONSOLE_API_IMAGE` | _(empty)_ | Custom Console API image. Overrides operator's default API image. |
| `CONSOLE_UI_IMAGE` | _(empty)_ | Custom Console UI image. Overrides operator's default UI image. |

**Use case:** Testing local changes to Console API or UI before they're released.

**Example:**
```yaml
CONSOLE_API_IMAGE: quay.io/your-username/console-api:my-feature
CONSOLE_UI_IMAGE: quay.io/your-username/console-ui:my-feature
```

### Component Versions

Versions of external components deployed during tests:

| Variable | Default | Description |
|----------|---------|-------------|
| `KROXYLICIOUS_VERSION` | `0.20.0` | Kroxylicious (Kafka proxy) release version for manifest downloads. |
| `APICURIO_VERSION` | `3.2.4` | Apicurio Registry (schema registry) version. |
| `KEYCLOAK_VERSION` | `26.4.0` | Keycloak (identity provider) release version. |
| `PROMETHEUS_VERSION` | `0.89.0` | Prometheus (metrics) operator release version. |
| `ST_KAFKA_VERSION` | `4.2.0` | Kafka version to use in test clusters. |
| `TEST_CLIENTS_IMAGE` | `quay.io/strimzi-test-clients/test-clients:0.14.0-kafka-4.2.0` | Container image for producer/consumer test clients. |

**Note:** Update versions when testing compatibility with newer component releases.

## Configuration Examples

### Debug Mode Configuration

For investigating test failures:

```yaml
CLEANUP_ENVIRONMENT: false       # Keep resources after test
RUN_HEADLESS: false              # Watch browser interactions
TEST_CONSOLE_LOG_LEVEL: DEBUG    # Detailed console output
TEST_FILE_LOG_LEVEL: TRACE       # Maximum file logging detail
```

### CI/Automation Configuration

For automated test execution:

```yaml
CLEANUP_ENVIRONMENT: true        # Always clean up
RUN_HEADLESS: true               # No GUI needed
TEST_CONSOLE_LOG_LEVEL: INFO     # Clean console output
```

### Custom Image Testing

Testing locally-built Console:

```yaml
CONSOLE_API_IMAGE: localhost:5000/console-api:dev
CONSOLE_UI_IMAGE: localhost:5000/console-ui:dev
```

### External Strimzi Configuration

Using pre-installed Strimzi:

```yaml
SKIP_STRIMZI_INSTALLATION: true
STRIMZI_OPERATOR_NAME: my-strimzi-cluster-operator
```

---

**See also:**
- [README.md](README.md) - Getting started guide
- [RUNNING_TESTS.md](RUNNING_TESTS.md) - How to run tests
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Debugging and troubleshooting
