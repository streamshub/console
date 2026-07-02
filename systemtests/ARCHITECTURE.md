# Test Framework Architecture

Understanding the testing infrastructure helps debug failures and develop new tests.

## Table of Contents

- [AbstractST - Base Class for All Tests](#abstractst---base-class-for-all-tests)
- [Setup Components](#setup-components)
- [Core Testing Frameworks](#core-testing-frameworks)
- [Test Buckets](#test-buckets)
- [Logging and Diagnostics](#logging-and-diagnostics)

## AbstractST - Base Class for All Tests

Every system test class extends `AbstractST`, which provides common setup and teardown functionality.

**What AbstractST does for you:**

When you write a test class that extends `AbstractST`, it automatically:
- Sets up the test environment before any tests run (operators, namespaces)
- Initializes the test framework and resource tracking
- Cleans up resources after tests complete (unless `CLEANUP_ENVIRONMENT: false`)

**How it works:**

`AbstractST` uses JUnit 5 lifecycle hooks (`@BeforeAll`, `@AfterAll`) to handle the heavy lifting of environment setup and teardown. Your test class just focuses on the actual test scenarios.

**Location:** `src/test/java/com/github/streamshub/systemtests/AbstractST.java`

**Test classes that extend AbstractST:**
- `MessagesST` - Message production/consumption tests
- `AuthST` - Authentication and authorization tests
- `ApicurioST` - Schema registry tests
- `KroxyST` - Kafka proxy tests
- And more...

## Setup Components

Component-specific setup managers located in `src/main/java/.../setup/`:

| Directory | Purpose |
|-----------|---------|
| `console/` | Console operator and Console instance deployment |
| `strimzi/` | Strimzi Cluster Operator (Kafka operator) installation |
| `keycloak/` | Keycloak identity provider for authentication tests |
| `apicurio/` | Apicurio schema registry for schema tests |
| `kroxylicious/` | Kroxylicious Kafka proxy setup |
| `prometheus/` | Prometheus metrics collection for monitoring tests |

Each setup manager knows how to:
- Deploy its component to Kubernetes
- Wait for component readiness
- Configure component for test scenarios
- Clean up after tests

## Core Testing Frameworks

### kubetest4j - Kubernetes Resource Management

**Purpose:** Manage Kubernetes resources in tests

**Key Features:**
- `KubeResourceManager` - Creates, tracks, and cleans up K8s resources automatically
- Defines resource types for all custom resources (Kafka, Console, Kroxy, etc.)
- Handles resource lifecycle: creation, waiting for readiness, deletion
- Supports both namespaced and cluster-scoped resources

**Example Usage in Tests:**
```java
// Resources are automatically tracked and cleaned up
KubeResourceManager.get().createResource(kafkaResource);
```

### Playwright - Browser Automation

**Purpose:** Automate browser interactions for UI testing

**Key Features:**
- Automates browser actions: clicks, typing, navigation, assertions
- Controlled via `RUN_HEADLESS` setting in config.yaml
- Captures screenshots on failures to `SCREENSHOTS_DIR_PATH`
- Generates detailed trace files for debugging (see [TROUBLESHOOTING.md](TROUBLESHOOTING.md))

**Browser Modes:**
- Headless (default): Tests run without visible browser - faster, suitable for CI
- Headed: Browser window visible - useful for debugging and test development

## Test Buckets

### `@TestBucket` annotation

**Purpose:** Group related tests that share Kubernetes resources

**Benefits:**
- Optimizes resource creation and cleanup
- Reduces test execution time by reusing infrastructure within a bucket
- Tests in the same bucket share setup/teardown phases

**Usage:**
```java
@TestBucket("messages-basic")
@Test
void testProduceMessages() {
    // Test implementation
}

@TestBucket("messages-basic")
@Test
void testConsumeMessages() {
    // Shares resources with testProduceMessages
}
```

## Logging and Diagnostics

The test framework automatically collects comprehensive logs, tracing and resources when tests fail.

### TestLogCollector

**Purpose:** Automatically collect resource description and logs on test failure

**What it collects:**
- Logs from all namespaces labeled with `COLLECT_ST_LOGS=true`
- Operator logs (Strimzi, Console operators)
- Application logs (Kafka brokers, Console instances)
- Test client logs
- Supporting service logs (Keycloak, Apicurio, etc.)

**Output location:** `TEST_LOG_DIR` (default: `target/logs/`)

**Automatic labeling:** All namespaces created by tests are automatically labeled for log collection

### YAML Resource Storage

**Purpose:** Save all created Kubernetes resources for post-failure analysis

**How it works:**
- Enabled via `KubeResourceManager.get().setStoreYamlPath(Environment.TEST_LOG_DIR)`
- Every K8s resource created during tests is saved as a YAML file
- Files are timestamped and organized by test run

**What's saved:**
- ConfigMaps and Secrets
- Deployments and StatefulSets
- Services and Ingresses
- Custom Resources (Kafka, KafkaTopic, KafkaUser, Console, etc.)

**Use case:** Reproduce issues by examining exact resource configurations after test failure

### Configuration Snapshot

**Purpose:** Capture test environment configuration for reproducibility

**How it works:**
- `Environment.logConfigAndSaveToFile()` saves current config at test initialization
- Creates a copy of `config.yaml` in `TEST_LOG_DIR`
- Includes all environment variable overrides

**Use case:** Understand what configuration was used for a particular test run

### Log Levels

Control logging verbosity independently for files and console:

| Variable | Default | Purpose |
|----------|---------|---------|
| `TEST_FILE_LOG_LEVEL` | `DEBUG` | File output verbosity (TRACE, DEBUG, INFO, WARN, ERROR) |
| `TEST_CONSOLE_LOG_LEVEL` | `INFO` | Console output verbosity |

**Strategy:** Detailed file logs (DEBUG) + clean console output (INFO) = best debugging experience

---

**See also:**
- [README.md](README.md) - Getting started guide
- [RUNNING_TESTS.md](RUNNING_TESTS.md) - How to run tests
- [CONFIGURATION.md](CONFIGURATION.md) - Configuration reference
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Debugging and troubleshooting
