# Console Operator

This repository contains the operator responsible for deploying the StreamsHub console API/UI deployment.

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
mvn -am -pl operator install -DskipTests
```

To run unit and integration tests

```shell
mvn -am -pl operator verify
```

### Strimzi and Prometheus

Follow the steps in the [install README](../install/README.md) to install the Strimzi and Prometheus operators and to
create instances of both Prometheus and Kafka.

### Start Console API in Development Mode

Start the API in development mode from the repository root directory.

```shell
mvn -am -pl operator quarkus:dev
```
