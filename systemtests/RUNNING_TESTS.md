# Running System Tests

This guide covers how to run tests, common testing scenarios, and practical examples.

## Table of Contents

- [Maven Commands](#maven-commands)
- [Skip Options](#skip-options)
- [Maven Profiles](#maven-profiles)
- [IDE Debugging](#ide-debugging)
- [Browser Modes](#browser-modes)
- [Debug Mode Best Practices](#debug-mode-best-practices)
- [Common Scenarios](#common-scenarios)

## Maven Commands

### Run All System Tests

```bash
mvn -pl systemtests verify
```

Executes all tests in `src/test/java/**/*ST.java` using maven-failsafe-plugin.

### Run Specific Test Class

```bash
mvn -pl systemtests verify -Dit.test=MessagesST
```

Runs all test methods in the `MessagesST` class.

**Available test classes:**
- `MessagesST` - Message production and consumption
- `AuthST` - Authentication flows
- `KafkaCredentialsST` - Kafka credential management
- `ApicurioST` - Schema registry integration
- `KroxyST` - Kafka proxy functionality
- `GroupsST` - Consumer group management
- `KafkaST` - Basic Kafka operations
- `PrometheusST` - Metrics collection
- `OlmUpgradeST`, `YamlUpgradeST` - Operator upgrades

### Run Specific Test Method

```bash
mvn -pl systemtests verify -Dit.test=MessagesST#testSendMessage
```

Runs only the `testSendMessage` method in `MessagesST`.

### Multiple Test Classes

```bash
mvn -pl systemtests verify -Dit.test=MessagesST,AuthST
```

Runs all tests in both `MessagesST` and `AuthST`.

## Skip Options

### Skip All Tests

```bash
# From project root - build without running any tests
mvn clean install -DskipTests
```

**Use case:** Fast build when you don't need test validation.

### Skip Only System Tests

```bash
mvn clean install -DskipSTs=true
```

**Use case:** Run unit tests but skip slower system tests.

### Skip Only Unit Tests

```bash
mvn clean install -DskipUTs=true
```

**Use case:** Run system tests only (assumes unit tests already pass).

## Maven Profiles

Check [pom.xml](pom.xml) for available profiles:

```bash
grep "<profile>" systemtests/pom.xml -A5
```

**Activate profiles:**
```bash
mvn -pl systemtests verify -P<profile-name>
```

**Combine multiple profiles:**
```bash
mvn -pl systemtests verify -P<profile1>,<profile2>
```

## IDE Debugging

Run and debug tests directly from your IDE for faster development cycles.

### IntelliJ IDEA

**Run a test:**
1. Open test class in editor (e.g., `MessagesST.java`)
2. Click green arrow next to test class or method
3. Select "Run" or "Debug"

**Debug with breakpoints:**
1. Set breakpoints by clicking left gutter in editor
2. Right-click test class or method
3. Select "Debug 'TestName'"
4. Use debugger controls (Step Over, Step Into, etc.)

**Run configuration:**
- IntelliJ auto-creates JUnit run configurations
- Modify VM options, environment variables in Run Configuration dialog
- Set working directory to `systemtests` module

### VS Code

**Prerequisites:**
- Install "Extension Pack for Java" extension
- Install "Test Runner for Java" extension

**Run a test:**
1. Open test class in editor
2. Click "Run Test" or "Debug Test" CodeLens above test method
3. View results in Test Explorer sidebar

**Debug with breakpoints:**
1. Set breakpoints by clicking left of line numbers
2. Click "Debug Test" for specific test
3. Use Debug toolbar (Continue, Step Over, Step Into, etc.)

## Browser Modes

Control Playwright browser visibility during UI tests.

### Headless Mode (Default)

```yaml
# In config.yaml
RUN_HEADLESS: true
```

```bash
# Or via environment variable
export RUN_HEADLESS=true
mvn -pl systemtests verify
```

**Characteristics:**
- Browser runs without visible window
- Faster execution (no GUI rendering overhead)
- Suitable for CI/CD pipelines
- Default for automated testing

### Headed Mode (Visual Debugging)

```yaml
# In config.yaml
RUN_HEADLESS: false
```

```bash
# Or via environment variable
export RUN_HEADLESS=false
mvn -pl systemtests verify -Dit.test=MessagesST
```

**When to use:**
- Debugging UI test failures
- Understanding test flow visually
- Developing new UI tests
- Verifying element selectors

**Benefits:**
- See exact browser interactions
- Watch page state changes
- Identify timing issues
- Verify element visibility

## Debug Mode Best Practices

Maximize debugging effectiveness with these settings:

### 1. Disable Cleanup

```yaml
CLEANUP_ENVIRONMENT: false
```

**Why:** Inspect Kubernetes resources after test failure.

**Post-test inspection:**
```bash
kubectl get all -A
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### 2. Increase Log Verbosity

```yaml
TEST_CONSOLE_LOG_LEVEL: DEBUG
TEST_FILE_LOG_LEVEL: TRACE
```

**Why:** See detailed operation logs for troubleshooting.

### 3. Run Single Test

```bash
mvn -pl systemtests verify -Dit.test=MessagesST#testSendMessage
```

**Why:** Avoid resource conflicts and focus on specific failure.

### 4. Use Headed Browser

```yaml
RUN_HEADLESS: false
```

**Why:** Watch UI interactions in real-time.

### 5. Check Diagnostic Output

After test failure, check `TEST_LOG_DIR`:

```bash
ls -la target/logs/
# Review:
# - Test execution logs
# - YAML resource dumps
# - Pod logs
# - Configuration snapshot
```

### Complete Debug Session Example

```bash
# 1. Configure for debugging
sed -i.bak \
  -e 's/^CLEANUP_ENVIRONMENT:.*/CLEANUP_ENVIRONMENT: false/' \
  -e 's/^RUN_HEADLESS:.*/RUN_HEADLESS: false/' \
  -e 's/^TEST_CONSOLE_LOG_LEVEL:.*/TEST_CONSOLE_LOG_LEVEL: DEBUG/' \
  systemtests/config.yaml

# 2. Run specific failing test
mvn -pl systemtests verify -Dit.test=MessagesST#testSendMessage

# 3. If test fails, inspect cluster
kubectl get all -A
kubectl logs -n <namespace> <console-pod>

# 4. Review diagnostics
ls -la systemtests/target/logs/
cat systemtests/target/logs/config.yaml  # Verify test configuration
cat systemtests/target/logs/<timestamp>/test-execution.log

# 5. View Playwright trace (see TROUBLESHOOTING.md)
open systemtests/tracing/<trace-file>.zip
# Upload to https://trace.playwright.dev/

# 6. Clean up manually when done
kubectl delete namespace <test-namespace>
```

## Common Scenarios

Practical examples for typical testing situations.

### Testing with Custom Console Images

**Use case:** Test your locally-built Console changes before creating a PR.

#### Build and Push Custom Images

See the [Makefile](../Makefile) in the project root for build and push commands.

#### Configure Tests to Use Custom Images

**IMPORTANT:** These settings must be configured **before running tests**. The API and UI images are used when deploying Console instances during test execution.

**Option 1: Edit config.yaml**

```yaml
CONSOLE_API_IMAGE: quay.io/your-username/console-api:my-feature
CONSOLE_UI_IMAGE: quay.io/your-username/console-ui:my-feature
```

**Option 2: Environment variables** (doesn't modify files)

```bash
export CONSOLE_API_IMAGE=quay.io/your-username/console-api:my-feature
export CONSOLE_UI_IMAGE=quay.io/your-username/console-ui:my-feature
mvn -pl systemtests verify -Dit.test=MessagesST
```

**Note:** For custom operator images, you need to rebuild the operator bundle (the operator image is sourced from `streamshub-console-operator.yaml` during test setup, not from config variables).

### Testing OLM vs YAML Installation

#### OLM Installation (Default)

**Use case:** Testing on OpenShift or validating operator bundle/catalog.

**Setup CatalogSource (if needed):**

If you need to set up a custom OLM catalog with a specific index image:

```bash
# Use the provided script
systemtests/scripts/setup-olm-catalog.sh --index-image quay.io/streamshub/console-index:latest

# Or with custom name/namespace
systemtests/scripts/setup-olm-catalog.sh \
  --index-image quay.io/streamshub/console-index:0.12.4 \
  --name my-catalog \
  --namespace openshift-marketplace
```

**Configure tests:**

```yaml
# In config.yaml
CONSOLE_INSTALL_TYPE: olm
CONSOLE_OLM_CHANNEL_NAME: alpha
CONSOLE_OLM_CATALOG_SOURCE_NAME: console-source  # Match catalog name
CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE: openshift-marketplace
CONSOLE_OLM_PACKAGE_NAME: streamshub-console-operator
```

**Run tests:**
```bash
mvn -pl systemtests verify
```

**What happens:**
- Tests expect CatalogSource to already exist (set up manually or via script above)
- Creates Subscription to operator in specified channel
- OLM installs operator automatically from the catalog
- Tests create Console instances via Custom Resources

**Verify CatalogSource:**
```bash
# Check CatalogSource exists
kubectl get catalogsource console-source -n openshift-marketplace

# Verify catalog pod is running
kubectl get pods -n openshift-marketplace | grep console-source

# List available operators from catalog
kubectl get packagemanifests | grep console
```

#### YAML Installation

**Use case:** Testing on non-OpenShift clusters or specific operator builds.

**Using released bundle:**

```yaml
# In config.yaml
CONSOLE_INSTALL_TYPE: yaml
CONSOLE_OPERATOR_BUNDLE_URL: https://github.com/streamshub/console/releases/download/0.12.4/streamshub-console-operator.yaml
```

**Using local bundle:**

```yaml
# In config.yaml
CONSOLE_INSTALL_TYPE: yaml
CONSOLE_OPERATOR_BUNDLE_URL: /absolute/path/to/console-operator.yaml
```

Or relative path from project root:

```yaml
CONSOLE_OPERATOR_BUNDLE_URL: operator/target/console-operator.yaml
```

**Run tests:**
```bash
mvn -pl systemtests verify
```

**What happens:**
- Downloads (or reads local) operator YAML manifests
- Applies manifests directly with kubectl
- Tests create Console instances via Custom Resources

### Testing with External Strimzi Installation

**Use case:** Strimzi is already installed in your cluster (shared cluster, pre-configured environment).

#### Configure to Skip Strimzi Installation

```yaml
# In config.yaml
SKIP_STRIMZI_INSTALLATION: true
STRIMZI_OPERATOR_NAME: my-strimzi-cluster-operator  # Match your deployment name
```

#### Verify Strimzi Deployment Name

```bash
kubectl get deployment -A | grep strimzi
# Output example: co-namespace  my-strimzi-cluster-operator  1/1  Running
```

Use the deployment name in `STRIMZI_OPERATOR_NAME`.

#### Run Tests

```bash
mvn -pl systemtests verify
```

**What happens:**
- Tests skip Strimzi operator installation
- Tests use existing Strimzi operator for Kafka cluster management
- Kafka resources (Kafka, KafkaTopic, KafkaUser) still created by tests

**Requirements:**
- Strimzi operator must be running
- Strimzi CRDs must be installed
- Operator must have permissions in test namespaces

### Testing Different Component Versions

**Use case:** Validate Console compatibility with different component versions.

Configure the versions in `config.yaml` before running tests:

```yaml
# Kafka version
ST_KAFKA_VERSION: 4.1.0  # Change from default 4.2.0
TEST_CLIENTS_IMAGE: quay.io/strimzi-test-clients/test-clients:0.14.0-kafka-4.1.0

# Apicurio Schema Registry version
APICURIO_VERSION: 3.2.3  # Change from default 3.2.4

# Strimzi Operator version
STRIMZI_OPERATOR_VERSION: 0.43.0  # Change from default
```

**Run tests:**
```bash
mvn -pl systemtests verify
```

**Validation:** Tests verify Console works correctly with the specified component versions.

---

**See also:**
- [README.md](README.md) - Getting started guide
- [CONFIGURATION.md](CONFIGURATION.md) - Configuration reference
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Debugging and troubleshooting
