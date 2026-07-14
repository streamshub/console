# StreamsHub Console System Tests

End-to-end UI tests for StreamsHub Console against real Kafka clusters managed by Strimzi in Kubernetes.

📖 **[Running Tests](RUNNING_TESTS.md)** · ⚙️ **[Configuration](CONFIGURATION.md)** · 🏗️ **[Architecture](ARCHITECTURE.md)** · 🔧 **[Troubleshooting](TROUBLESHOOTING.md)**

## What's tested

- Console UI interactions with Kafka clusters
- Authentication and authorization flows
- Message production and consumption
- Schema registry integration (Apicurio)
- Kafka proxy functionality (Kroxylicious)
- Metrics and monitoring (Prometheus)
- Operator lifecycle and upgrades

**Stack:** JUnit 5, Playwright, [kubetest4j](https://github.com/skodjob/kubetest4j) (Kubernetes resource management), Maven.

## Quick Start

```bash
# From project root
mvn clean install -DskipTests          # build all modules the tests depend on (API, UI, CRDs)
mvn -pl systemtests verify -DskipSTs=false
```

System tests are skipped by default (`-DskipSTs=false` is required — see [Skip Options](RUNNING_TESTS.md#skip-options)).

Once the full project is built once, iterate faster by rebuilding just this module:
```bash
mvn -pl systemtests clean verify -DskipSTs=false
```

If this fails, check [Prerequisites](#prerequisites) below.

## Prerequisites

| Software | Version | Notes |
|----------|---------|-------|
| **Java JDK** | 21 | [OpenJDK](https://openjdk.org/) |
| **Maven** | 3.8+ | [Apache Maven](https://maven.apache.org/install.html) |
| **Kubernetes cluster** | - | minikube, kind, k3s, or OpenShift |
| **kubectl or oc** | matches your cluster | [kubectl](https://kubernetes.io/docs/tasks/tools/), [oc](https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.html) |

**Cluster resources:** at least 4 CPU / 8GB RAM (6 CPU / 12GB recommended — Kafka test clusters run 6 nodes). 20GB+ free disk for images and volumes.

For CI environment details (Ubuntu + minikube), see `.github/workflows/` in the repository root.

## Environment Setup

### Kubernetes Cluster Setup

Tests create and clean up their own namespaces automatically. Namespaces are labeled `COLLECT_ST_LOGS=true` so failure logs get collected; cleanup is controlled by `CLEANUP_ENVIRONMENT` in `config.yaml`.


## Next Steps

1. **[Run Tests](RUNNING_TESTS.md)** - Maven commands, IDE debugging, common scenarios
2. **[Configuration](CONFIGURATION.md)** - `config.yaml` reference
3. **[Architecture](ARCHITECTURE.md)** - test framework internals
4. **[Troubleshooting](TROUBLESHOOTING.md)** - debugging failures, CI/CD
