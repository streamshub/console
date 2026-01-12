# Console API

This repository contains the Console API server to interact with [Strimzi](https://strimzi.io) Kafka instances running
in a Kubernetes cluster.

The API provides a way to manage [Apache Kafka<sup>®</sup>](https://kafka.apache.org/) topics and consumer groups, as
well as endpoints to publish and browse messages.

## Getting Started

### _Prerequisites_

There are a few things you need to have installed to run this project:

- [Maven](https://maven.apache.org/)
- [JDK 21+](https://openjdk.java.net/projects/jdk/21/)
- Kubernetes environment ([minikube](https://minikube.sigs.k8s.io/) recommended) with [Strimzi Cluster Operator](https://strimzi.io) installed and a Kafka cluster deployed
- [Docker](https://www.docker.com/) or [Podman](https://podman.io)

### Download and Build

To run this project locally, first clone it with Git and execute a Maven build

```shell
git clone git@github.com:streamshub/console.git
cd console
mvn -am -pl api install -DskipTests
```

To run unit and integration tests

```shell
mvn -am -pl api verify
```

### Strimzi and Prometheus

Before proceeding, please make sure the [prerequisites listed in the main README](https://github.com/streamshub/console/blob/main/api/README.md#strimzi-and-prometheus) are met. to install the Strimzi and Prometheus operators and to create instances of both Prometheus and Kafka.

### Start Console API in Development Mode

Start the API in development mode from the repository root directory. Ensure that the config-path given points to a
valid `console-config.yaml`. See [console-config-example.yaml](../console-config-example.yaml) for an example.

```shell
mvn -am -pl api quarkus:dev -Dconsole.config-path=$(pwd)/console-config.yaml
```

### Using the Instance API

Once all steps above have been completed, you can interact with the Kafka Instance API. The server will start the following interfaces:
- REST API on [http://localhost:8080](http://localhost:8080)
- Metrics at [http://localhost:8080/metrics](http://localhost:8080/metrics)
- Health status at [http://localhost:8080/health](http://localhost:8080/health)
- OpenAPI at [http://localhost:8080/openapi](http://localhost:8080/openapi?format=json)
- Swagger UI at [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)

## Logging Configuration Override
The container image built from this repository includes support for providing an additional logging configuration at run time (without requiring a restart).
Configuration property `logging.config.override` (or environment variable `LOGGING_CONFIG_OVERRIDE`) must be provided to the server at startup with a path to
the logging configuration override file that _may_ exist during run time. When created, the file may contain any valid Quarkus [log category level configurations](https://quarkus.io/guides/logging#logging-categories).
