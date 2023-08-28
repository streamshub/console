[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eyefloaters_console-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eyefloaters_console-api) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=eyefloaters_console-api&metric=coverage)](https://sonarcloud.io/summary/new_code?id=eyefloaters_console-api)

# Console API

This repository contains the Console API server to interact with [Strimzi](https://strimzi.io) Kafka instances running
in a Kubernetes cluster.

The API provides a way to manage [Apache Kafka<sup>TM</sup>](https://kafka.apache.org/) topics and consumer groups, as
well as endpoints to publish and browse messages.

## Getting Started

### _Prerequisites_

There are a few things you need to have installed to run this project:

- [Maven](https://maven.apache.org/)
- [JDK 17+](https://openjdk.java.net/projects/jdk/17/)
- Kubernetes environment ([minikube](https://minikube.sigs.k8s.io/) recommended) with [Strimzi Cluster Operator](https://strimzi.io) installed and a Kafka cluster deployed
- [Docker](https://www.docker.com/) or [Podman](https://podman.io)

### Download and Build

To run this project locally, first clone it with Git and execute a Maven build

```shell
git clone git@github.com:eyefloaters/console-api.git
cd console-api
mvn install -DskipTests
```

### Enable Minikube with Ingress

Start minikube and enable the ingress controller with passthrough TLS

```shell
minikube start
minikube addons enable ingress
kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
 --type='json' \
 -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
```

### Install Strimzi and Kafka Cluster

Create the Strimzi Operator artifacts and two Kafka clusters in the `myproject` namespace.

```shell
kubectl create namespace myproject
curl -L https://github.com/strimzi/strimzi-kafka-operator/releases/download/0.36.1/strimzi-cluster-operator-0.36.1.yaml | kubectl apply -f - -n myproject
kubectl wait deployment strimzi-cluster-operator -n myproject --for condition=Available
sed -e 's/\${cluster_name}/cluster-a/g' -e 's/\${kube_ip}/'$(minikube ip)'/g' examples/kafka-ephemeral-ingress.yaml | kubectl apply -f - -n myproject
sed -e 's/\${cluster_name}/cluster-b/g' -e 's/\${kube_ip}/'$(minikube ip)'/g' examples/kafka-ephemeral-ingress.yaml | kubectl apply -f - -n myproject
```

### Start Console API in Development Mode

```shell
mvn quarkus:dev
```

### Using the Instance API

Once all steps above have been completed, you can interact with the Kafka Instance API. The server will start the following interfaces:
- REST API on [http://localhost:8080](http://localhost:8080)
- Metrics at [http://localhost:8080/metrics](http://localhost:8080/metrics)
- Health status at [http://localhost:8080/health](http://localhost:8080/health)
- OpenAPI at [http://localhost:8080/openapi](http://localhost:8080/openapi?format=json)
- Swagger UI at [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)

## Releasing

### Milestones
Each release requires an open milestone that includes the issues/pull requests that are part of the release. All issues in the release milestone must be closed. The name of the milestone must match the version number to be released.

### Configuration
The release action flow requires that the following secrets are configured in the repository:
* `IMAGE_REPO_HOSTNAME` - the host (optionally including a port number) of the image repository where images will be pushed
* `IMAGE_REPO_NAMESPACE` - namespace/library/user where the image will be pushed
* `IMAGE_REPO_USERNAME` - user name for authentication to server `IMAGE_REPO_HOSTNAME`
* `IMAGE_REPO_PASSWORD` - password for authentication to server `IMAGE_REPO_HOSTNAME`
These credentials will be used to push the release image to the repository configured in the `.github/workflows/release.yml` workflow.

### Performing the Release
Releases are performed by modifying the `.github/project.yml` file, setting `current-version` to the release version and `next-version` to the next SNAPSHOT. Open a pull request with the changed `project.yml` to initiate the pre-release workflows. At this phase, the project milestone will be checked and it will be verified that no issues for the release milestone are still open. Additionally, the project's integration test will be run.
Once approved and the pull request is merged, the release action will execute. This action will execute the Maven release plugin to tag the release commit, build the application artifacts, create the build image, and push the image to (currently) quay.io. If successful, the action will push the new tag to the Github repository and generate release notes listing all of the closed issues included in the milestone. Finally, the milestone will be closed.

## Logging Configuration Override
The container image built from this repository includes support for providing an additional logging configuration at run time (without requiring a restart).
Configuration property `logging.config.override` (or environment variable `LOGGING_CONFIG_OVERRIDE`) must be provided to the server at startup with a path to
the logging configuration override file that _may_ exist during run time. When created, the file may contain any valid Quarkus [log category level configurations](https://quarkus.io/guides/logging#logging-categories).
