# Running System Tests

## Table of Contents

- [Maven Commands](#maven-commands)
- [Skip Options](#skip-options)
- [Maven Profiles](#maven-profiles)
- [IDE Debugging](#ide-debugging)
- [Browser Modes](#browser-modes)
- [Debug Mode Best Practices](#debug-mode-best-practices)
- [Common Scenarios](#common-scenarios)

## Maven Commands

> System tests are skipped by default (the `none` Maven profile is active unless you override it — see [Skip Options](#skip-options) and [Maven Profiles](#maven-profiles)). Every command below needs `-DskipSTs=false` or an activated profile like `-Pregression` to actually run.

### Run All System Tests

```bash
mvn -pl systemtests verify -DskipSTs=false
```

Executes all tests in `src/test/java/**/*ST.java` via maven-failsafe-plugin.

### Run a Specific Test Class

```bash
mvn -pl systemtests verify -DskipSTs=false -Dit.test=MessagesST
```

**Available test classes:** `MessagesST`, `AuthST`, `KafkaCredentialsST`, `ApicurioST`, `KroxyST`, `GroupsST`, `KafkaST`, `PrometheusST`, `OlmUpgradeST`, `YamlUpgradeST`.

### Run a Specific Test Method

```bash
mvn -pl systemtests verify -DskipSTs=false -Dit.test=MessagesST#testSendMessage
```

### Multiple Test Classes

```bash
mvn -pl systemtests verify -DskipSTs=false -Dit.test=MessagesST,AuthST
```

## Skip Options

| Command (from project root) | Effect |
|---|---|
| `mvn clean install -DskipTests` | Build only, no tests |
| `mvn clean install -DskipSTs=true` | Run unit tests, skip system tests |
| `mvn clean install -DskipUTs=true` | Run system tests, skip unit tests |
| `mvn -pl systemtests verify -DskipSTs=false` | Run system tests (default profile skips them otherwise) |

## Maven Profiles

`systemtests/pom.xml` defines profiles that set `skipTests`/JUnit tag groups together, so you don't need `-DskipSTs=false` when using one:

| Profile | Runs |
|---|---|
| `none` (default) | Nothing — system tests are skipped |
| `regression` | Tests tagged `regression` |
| `olm-upgrade` / `yaml-upgrade` | Operator upgrade tests |
| `upgrade` | Both upgrade profiles |
| `all` | `regression`, `olm-upgrade`, `yaml-upgrade` |

```bash
mvn -pl systemtests verify -Pregression
mvn -pl systemtests verify -Pall
```

## IDE Debugging

### IntelliJ IDEA

1. Open the test class (e.g. `MessagesST.java`)
2. Click the gutter arrow next to the class or method → **Run** or **Debug**
3. Set breakpoints in the editor gutter; use the debugger's Step Over/Into controls
4. Adjust VM options or env vars in the generated Run Configuration if needed

### VS Code

Requires the "Extension Pack for Java" and "Test Runner for Java" extensions.

1. Open the test class
2. Click **Run Test** / **Debug Test** above the method (CodeLens)
3. Set breakpoints left of the line numbers; view results in the Test Explorer sidebar

## Browser Modes

```yaml
# config.yaml
RUN_HEADLESS: true   # default — no visible browser, faster, CI-friendly
RUN_HEADLESS: false  # visible browser — debugging, watching test flow, verifying selectors
```

Or via environment variable, without editing the file:
```bash
export RUN_HEADLESS=false
mvn -pl systemtests verify -DskipSTs=false -Dit.test=MessagesST
```

## Debug Mode Best Practices

```yaml
CLEANUP_ENVIRONMENT: false     # keep K8s resources after failure for inspection
RUN_HEADLESS: false            # watch browser interactions
TEST_CONSOLE_LOG_LEVEL: DEBUG
TEST_FILE_LOG_LEVEL: TRACE     # maximum detail in target/logs/
```

Run a single test to avoid resource conflicts and isolate the failure:
```bash
mvn -pl systemtests verify -DskipSTs=false -Dit.test=MessagesST#testSendMessage
```

## Common Scenarios

### Testing with a Custom Console API Image

**Use case:** validate a locally-built API change before opening a PR.

Build and push it (see the [Makefile](../Makefile) in the project root), then point tests at it — either edit `config.yaml`:

```yaml
CONSOLE_API_IMAGE: quay.io/streamshub/console-api:latest
```

or set it as an environment variable for a single run:

```bash
export CONSOLE_API_IMAGE=quay.io/streamshub/console-api:latest
mvn -pl systemtests verify -DskipSTs=false -Dit.test=MessagesST
```

This must be set **before** running tests — the image is used when tests deploy Console instances. For custom operator images, rebuild the operator bundle instead (`CONSOLE_OPERATOR_BUNDLE_URL`); the operator image isn't controlled by a config variable.

### Testing OLM vs YAML Installation

#### OLM (default)

**Use case:** testing on OpenShift, or validating the operator bundle/catalog.

A CatalogSource can be deplyed in the cluster manually using commands below, but STs can handle the creation when you set env `CONSOLE_OLM_CATALOG_SOURCE_IMAGE`. 

**On OpenShift**, the marketplace namespace already exists:

```bash
kubectl apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: console-source
  namespace: openshift-marketplace
spec:
  sourceType: grpc
  image: quay.io/streamshub/console-operator-catalog:latest
EOF
```

```yaml
# config.yaml
CONSOLE_INSTALL_TYPE: olm
CONSOLE_OLM_CHANNEL_NAME: alpha
CONSOLE_OLM_CATALOG_SOURCE_NAME: console-source
CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE: openshift-marketplace
CONSOLE_OLM_PACKAGE_NAME: streamshub-console-operator
```

**On minikube/kind** there's no `openshift-marketplace` namespace — enable OLM and use its `olm` namespace instead (this is what CI does):

```bash
minikube addons enable olm
kubectl apply -n olm -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: console-source
spec:
  sourceType: grpc
  image: quay.io/streamshub/console-operator-catalog:latest
EOF
```

```yaml
# config.yaml
CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE: olm
```

```bash
mvn -pl systemtests verify -DskipSTs=false
```

Tests automaticaly create a Subscription in the given channel; OLM installs the operator from the catalog, and tests create Console instances via Custom Resources.

**Verify** (substitute your CatalogSource namespace, `openshift-marketplace` or `olm`):
```bash
kubectl get catalogsource console-source -n <namespace>
kubectl get pods -n <namespace> | grep console-source
# View present manifests using:
kubectl get packagemanifests | grep console
```

#### YAML

**Use case:** non-OpenShift clusters, or testing a specific operator build.

```yaml
# config.yaml — released bundle
CONSOLE_INSTALL_TYPE: yaml
CONSOLE_OPERATOR_BUNDLE_URL: https://github.com/streamshub/console/releases/download/0.12.4/streamshub-console-operator.yaml
```
 or a local build (see .github/workflows/systemtests.yml for how CI generates this file)
```yaml
CONSOLE_OPERATOR_BUNDLE_URL: target/streamshub-console-operator.yaml
```

```bash
mvn -pl systemtests verify -DskipSTs=false
```

Tests download (or read) the manifests and apply them with `kubetest4j`, then they create Console instances via Custom Resources.

### Testing with an Externally-Installed Strimzi

**Use case:** Strimzi is already running in a `co-namespace` shared namespace for testing cluster operators.

```yaml
# config.yaml
SKIP_STRIMZI_INSTALLATION: true
STRIMZI_OPERATOR_NAME: strimzi-cluster-operator  # match your deployment's name
```

Find the deployment name if unsure:
```bash
kubectl get deployment -A | grep strimzi
# co-namespace  strimzi-cluster-operator  1/1  Running
```

```bash
mvn -pl systemtests verify -DskipSTs=false
```

Tests skip installing Strimzi if already present and use the existing operator; Kafka resources (`Kafka`, `KafkaTopic`, `KafkaUser`) are still created per-test. Requires the operator to be running, its CRDs installed, and permissions in test namespaces.

### Testing Different Component Versions

**Use case:** validate Console against other component versions.

```yaml
# config.yaml
ST_KAFKA_VERSION: 4.1.0
TEST_CLIENTS_IMAGE: quay.io/strimzi-test-clients/test-clients:0.14.0-kafka-4.1.0
APICURIO_VERSION: 3.2.3
STRIMZI_OPERATOR_VERSION: 0.43.0
```

```bash
mvn -pl systemtests verify -DskipSTs=false
```

---

**See also:** [README.md](README.md) · [CONFIGURATION.md](CONFIGURATION.md) · [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
