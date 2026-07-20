# System Tests Configuration Reference

All configuration options in `systemtests/config.yaml`, with their code-level defaults and how to override them.

## Table of Contents

- [How to Override Configuration](#how-to-override-configuration)
- [Common Settings](#common-settings)
- [OLM Configuration](#olm-configuration)
- [YAML Installation Configuration](#yaml-installation-configuration)
- [Console Instance Images](#console-instance-images)
- [Component Versions](#component-versions)
- [Configuration Examples](#configuration-examples)

## How to Override Configuration

**Edit [`config.yaml`](config.yaml)** for persistent changes:
```yaml
RUN_HEADLESS: false
TEST_CONSOLE_LOG_LEVEL: DEBUG
```

**Or set an environment variable** for a one-off run — this takes precedence over `config.yaml`:
```bash
export RUN_HEADLESS=false
mvn -pl systemtests verify -DskipSTs=false
```

## Common Settings

| Variable | Default | Description |
|----------|---------|-------------|
| `CLEANUP_ENVIRONMENT` | `true` | Delete Kubernetes resources after each test. Set `false` to inspect cluster state after a failure. |
| `DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL` | `true` | Re-deploy the Console operator if it already exists in the cluster. |
| `TEST_FILE_LOG_LEVEL` | `DEBUG` | File log level: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`. |
| `TEST_CONSOLE_LOG_LEVEL` | `INFO` | Console log level — kept quiet while files carry detail. |
| `TEST_LOG_DIR` | `target/logs` | Test logs, YAML dumps, and diagnostics. Wiped by `mvn clean` (it's under `target/`). |
| `RUN_HEADLESS` | `true` | Run Playwright without a visible browser window. |
| `SCREENSHOTS_DIR_PATH` | `screenshots` | Failure screenshots, relative to `systemtests/`. **Not** under `target/`, so `mvn clean` won't remove them. |
| `SKIP_STRIMZI_INSTALLATION` | `false` | Skip installing Strimzi if it's already present in the cluster. |
| `STRIMZI_OPERATOR_NAME` | `strimzi-cluster-operator` | Strimzi deployment name — update when using an external Strimzi. |
| `CONSOLE_DEPLOYMENT_NAME` | `streamshub-console` | Console instance deployment name. |
| `CONSOLE_INSTALL_TYPE` | `olm` | `olm` (Operator Lifecycle Manager) or `yaml` (direct manifests). |
| `CLIENT_TYPE` | `kubectl` | `kubectl` or `oc` (OpenShift). |
| `CONSOLE_OPERATOR_VERSION` | Maven project version | Version of the new operator used for upgrade testing |
| `CONSOLE_CLUSTER_DOMAIN` | _none — must be set_ | Ingress domain for non-OpenShift clusters. Auto-detected on OpenShift; otherwise used verbatim, with no auto-detection. For minikube, compute it per-cluster: `$(minikube ip).nip.io`. `config.yaml` ships `192.168.49.2.nip.io` as an example only — it's one specific minikube instance's IP and will not match yours. |

## OLM Configuration

| Variable | Default                       | Description |
|----------|-------------------------------|-------------|
| `CONSOLE_OLM_CATALOG_SOURCE_NAME` | `console-source`              | CatalogSource name for the Console Operator. |
| `CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE` | `olm`                         | Namespace of the CatalogSource. |
| `CONSOLE_OLM_PACKAGE_NAME` | `streamshub-console-operator` | Package name in the operator catalog. |
| `CONSOLE_OLM_CHANNEL_NAME` | `alpha`                       | Subscription channel. |
| `CONSOLE_OLM_CATALOG_SOURCE_IMAGE` | _(empty)_                     | When set, tests create the CatalogSource automatically using this image before installing the operator, instead of requiring it to be deployed manually. |

Use OLM when testing on OpenShift or validating the operator bundle/catalog.

## YAML Installation Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `STRIMZI_OPERATOR_VERSION` | `1.0.0` | Strimzi Cluster Operator version to install. |
| `CONSOLE_OPERATOR_BUNDLE_URL` | [`.../streamshub-console-operator.yaml`](https://github.com/streamshub/console/releases/download/0.12.4/streamshub-console-operator.yaml) | URL, project-relative path, or absolute path to `console-operator.yaml`. |

Use YAML when testing on non-OpenShift clusters (minikube, kind, k3s) or a specific operator build.

## Console Instance Images

| Variable | Default | Description |
|----------|---------|-------------|
| `CONSOLE_API_IMAGE` | _(empty)_ | Overrides the operator's default Console API image — for testing local API changes before release. |

```yaml
CONSOLE_API_IMAGE: quay.io/streamshub/console-api:latest
```

## Component Versions

| Variable | Default | Description |
|----------|---------|-------------|
| `KROXYLICIOUS_VERSION` | `0.21.0` | Kroxylicious (Kafka proxy) manifest version. |
| `APICURIO_VERSION` | `3.2.4` | Apicurio Registry (schema registry) version. |
| `KEYCLOAK_VERSION` | `26.4.0` | Keycloak (identity provider) version. |
| `PROMETHEUS_VERSION` | `0.89.0` | Prometheus operator version. |
| `ST_KAFKA_VERSION` | `4.2.0` | Kafka version used in test clusters. |
| `TEST_CLIENTS_IMAGE` | `quay.io/strimzi-test-clients/test-clients:0.14.0-kafka-4.2.0` | Producer/consumer test client image. |

Update these to validate Console against a different component release.

## Configuration Examples

**Debugging a failure:**
```yaml
CLEANUP_ENVIRONMENT: false
RUN_HEADLESS: false
TEST_CONSOLE_LOG_LEVEL: DEBUG
TEST_FILE_LOG_LEVEL: TRACE
```

**CI/automation:**
```yaml
CLEANUP_ENVIRONMENT: true
RUN_HEADLESS: true
TEST_CONSOLE_LOG_LEVEL: INFO
```

**Locally-built Console API:**
```yaml
CONSOLE_API_IMAGE: quay.io/streamshub/console-api:latest
```

**External Strimzi:**
```yaml
SKIP_STRIMZI_INSTALLATION: true
STRIMZI_OPERATOR_NAME: strimzi-cluster-operator  # match your deployment's name
```

---

**See also:** [README.md](README.md) · [RUNNING_TESTS.md](RUNNING_TESTS.md) · [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
