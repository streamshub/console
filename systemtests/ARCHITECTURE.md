# Test Framework Architecture

## Table of Contents

- [AbstractST - Base Class for All Tests](#abstractst---base-class-for-all-tests)
- [Setup Components](#setup-components)
- [Core Testing Frameworks](#core-testing-frameworks)
- [Test Buckets](#test-buckets)
- [Logging and Diagnostics](#logging-and-diagnostics)

## AbstractST - Base Class for All Tests

Every test class extends `AbstractST` (`src/test/java/com/github/streamshub/systemtests/AbstractST.java`), which uses JUnit 5's `@BeforeAll`/`@AfterAll` to set up operators and namespaces before tests run and tear them down afterward (unless `CLEANUP_ENVIRONMENT: false`). Test classes just implement the scenarios.

Extends `AbstractST`: `MessagesST`, `AuthST`, `ApicurioST`, `KroxyST`, and the other `*ST` classes.

## Setup Components

Component-specific setup managers live in `src/main/java/.../setup/`, each deploying, waiting for readiness, configuring, and cleaning up its component:

| Directory | Component |
|-----------|-----------|
| `console/` | Console operator and Console instance |
| `strimzi/` | Strimzi Cluster Operator |
| `keycloak/` | Keycloak (authentication tests) |
| `apicurio/` | Apicurio schema registry |
| `kroxylicious/` | Kroxylicious Kafka proxy |
| `prometheus/` | Prometheus metrics |

## Core Testing Frameworks

### kubetest4j

Manages Kubernetes resources in tests via `KubeResourceManager` — creation, readiness waits, and automatic cleanup for both namespaced and cluster-scoped resources.

```java
// Automatically tracked and cleaned up
KubeResourceManager.get().createResource(kafkaResource);
```

### Playwright

Automates browser interactions (clicks, typing, navigation, assertions), controlled by `RUN_HEADLESS` in `config.yaml`:
- **Headless** (default) — no visible window, faster, CI-friendly
- **Headed** — visible browser, for debugging and test development

On failure it captures a screenshot to `systemtests/screenshots/<run-timestamp>/` and a trace file to `systemtests/tracing/<run-timestamp>/` (see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)). Both directories live outside `target/`, so `mvn clean` never removes them — useful for comparing screenshots/traces across runs.

## Test Buckets

`@TestBucket("name")` groups tests that share Kubernetes resources, so setup/teardown for the bucket runs once instead of per-test:

```java
@TestBucket("messages-basic")
@Test
void testProduceMessages() { ... }

@TestBucket("messages-basic")
@Test
void testConsumeMessages() { ... }  // shares resources with testProduceMessages
```

## Logging and Diagnostics

### TestLogCollector

On test failure, `TestLogCollector` collects logs from every namespace labeled `COLLECT_ST_LOGS=true` (applied automatically to all test-created namespaces) — operator logs, application logs (Kafka, Console), test client logs, and supporting services (Keycloak, Apicurio, etc.) — into `TEST_LOG_DIR` (default `target/logs/`, wiped by `mvn clean`).

### YAML Resource Storage

Every Kubernetes resource created during a test (ConfigMaps, Secrets, Deployments, Services, Custom Resources, etc.) is saved as a timestamped YAML file under `TEST_LOG_DIR` via `KubeResourceManager.get().setStoreYamlPath(Environment.TEST_LOG_DIR)`, so you can inspect the exact configuration after a failure.

### Configuration Snapshot

`Environment.logConfigAndSaveToFile()` copies the resolved `config.yaml` (including env var overrides) into `TEST_LOG_DIR` at test start, so you can confirm what configuration a given run used.

### Log Levels

| Variable | Default | Purpose |
|----------|---------|---------|
| `TEST_FILE_LOG_LEVEL` | `DEBUG` | File output verbosity |
| `TEST_CONSOLE_LOG_LEVEL` | `INFO` | Console output verbosity |

Detailed file logs + a quiet console is the default and recommended combination.

---

**See also:** [README.md](README.md) · [RUNNING_TESTS.md](RUNNING_TESTS.md) · [CONFIGURATION.md](CONFIGURATION.md) · [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
