# StreamsHub Console System Tests

End-to-end UI testing for StreamsHub Console with real Kafka clusters managed by Strimzi in Kubernetes.

## Quick Links

📖 **[Running Tests](RUNNING_TESTS.md)** - Maven commands, IDE debugging, common scenarios  
⚙️ **[Configuration](CONFIGURATION.md)** - Complete config.yaml reference  
🏗️ **[Architecture](ARCHITECTURE.md)** - Test framework internals and components  
🔧 **[Troubleshooting](TROUBLESHOOTING.md)** - Debugging, test output, common issues, CI/CD  

## Table of Contents

- [Introduction](#introduction)
- [Quick Start](#quick-start)
- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Next Steps](#next-steps)

## Introduction

The systemtests module provides end-to-end UI testing for StreamsHub Console. These tests validate the Console UI with real Kafka clusters managed by Strimzi Cluster Operator in Kubernetes, simulating real user scenarios for comprehensive validation.

**What we test:**
- Console UI interactions with Kafka clusters
- Authentication and authorization flows
- Message production and consumption
- Schema registry integration (Apicurio)
- Kafka proxy functionality (Kroxylicious)
- Metrics and monitoring (Prometheus)
- Operator lifecycle and upgrades

**Technology Stack:**
- **JUnit 5** - Test framework with lifecycle management
- **Playwright** - Browser automation for UI testing
- **kubetest4j** - Kubernetes resource management and test utilities
- **Maven** - Build and test execution

## Quick Start

For developers with environment already set up:

```bash
# From project root
# 1. Build the entire project (required - systemtests depend on other modules)
mvn clean install -DskipTests

# 2. Run system tests
mvn -pl systemtests verify
```

**Why build the whole project first?**

The systemtests module depends on artifacts from other modules in the project:
- [Console API module](../api/)
- [Console UI](../api/src/main/webui/)
- Shared libraries and utilities
- Custom resource definitions

Building the whole project ensures all dependencies are available in your local Maven repository.

**If tests fail:** Check [Prerequisites](#prerequisites) and [Environment Setup](#environment-setup) sections below.

## Prerequisites

### Required Software

| Software | Minimum Version | Recommended | Installation Guide |
|----------|----------------|-------------|-------------------|
| **Java JDK** | 17 or higher | 21 | [OpenJDK](https://openjdk.org/) |
| **Maven** | 3.8+ | Latest 3.x | [Apache Maven](https://maven.apache.org/install.html) |
| **Kubernetes Cluster** | - | minikube, kind, or OpenShift | See available options below |
| **kubectl or oc CLI** | Match your cluster | Latest | [kubectl](https://kubernetes.io/docs/tasks/tools/), [oc](https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.html) |

### Kubernetes Cluster Options

**Available options:**

| Option | Best For | Installation Guide |
|--------|----------|-------------------|
| **minikube** | Local development, Mac/Linux/Windows | [minikube docs](https://minikube.sigs.k8s.io/docs/start/) |
| **kind** | Lightweight, CI environments | [kind docs](https://kind.sigs.k8s.io/docs/user/quick-start/) |
| **k3s** | Lightweight, Linux | [k3s docs](https://k3s.io/) |
| **OpenShift** | Enterprise, production-like testing | [OpenShift docs](https://docs.openshift.com/) |

### Cluster Resource Requirements

| Configuration | CPU Cores | RAM | Notes |
|--------------|-----------|-----|-------|
| **Minimum** | 4 | 8GB | May experience timeouts with complex tests |
| **Recommended** | 6 | 12GB | Comfortable for most test scenarios (Kafka runs 6 nodes: 3 brokers + 3 controllers) |
| **Optimal** | 8+ | 16GB+ | Parallel test execution, minimal timeouts |

**Storage:** Sufficient disk space for container images and persistent volumes (at least 20GB free)

### GitHub Workflow Reference

For CI environment details (Ubuntu + minikube setup), see `.github/workflows/` in the repository root.

## Environment Setup

### Initial Project Build

**CRITICAL:** You must build the entire project before running system tests.

```bash
# From project root
mvn clean install -DskipTests
```

**Why this is required:**

The systemtests module depends on artifacts from other modules:
- [Console API module](../api/)
- [Console UI](../api/src/main/webui/)
- Shared libraries and utilities
- Custom resource definitions (CRDs)

These dependencies must be built and installed to your local Maven repository before systemtests can compile and run.

**When to rebuild the full project:**
- ✅ First time setting up your development environment
- ✅ After making changes to the UI or API
- ✅ After pulling changes that affect other modules (API, UI, CRDs)
- ✅ After switching branches with significant changes
- ✅ After `mvn clean` in the root directory

**For iterative test development:**

Once the full project is built, you can rebuild just systemtests:

```bash
mvn -pl systemtests clean verify
```

This is faster for repeated test runs during development.

### Kubernetes Cluster Setup

#### Namespace Considerations

**Automatic namespace management:**
- Tests create their own namespaces automatically
- Namespaces are labeled with `COLLECT_ST_LOGS=true` for log collection
- Cleanup is controlled by `CLEANUP_ENVIRONMENT` setting in config.yaml

**Manual cleanup (if needed):**
```bash
# List test namespaces
kubectl get namespaces | grep -E "test|console|kafka"

# Delete specific namespace
kubectl delete namespace <namespace-name>
```

## Next Steps

Now that you have your environment set up:

1. **[Run Tests](RUNNING_TESTS.md)** - Learn how to execute tests with Maven, IDE, and different configurations
2. **[Configuration](CONFIGURATION.md)** - Customize test behavior with config.yaml settings
3. **[Architecture](ARCHITECTURE.md)** - Understand the test framework internals
4. **[Troubleshooting](TROUBLESHOOTING.md)** - Debug test failures and resolve common issues

**Common workflows:**
- Testing custom builds: See [Custom Console Images](RUNNING_TESTS.md#testing-with-custom-console-images) in RUNNING_TESTS.md
- Debugging failures: See [Finding Root Cause](TROUBLESHOOTING.md#finding-root-cause-of-test-failures) in TROUBLESHOOTING.md
