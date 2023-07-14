[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eyefloaters_console-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eyefloaters_console-api)[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=eyefloaters_console-api&metric=coverage)](https://sonarcloud.io/summary/new_code?id=eyefloaters_console-api)

# Console API

This repository contains the Managed Kafka Instance API and its implementation.
The API provides a way for managing [Apache Kafka<sup>TM</sup>](https://kafka.apache.org/) topics and consumer groups, as
well as endpoints to publish and browse messages.

## Getting Started

###_Prerequisites_

There are a few things you need to have installed to run this project:

- [Maven](https://maven.apache.org/)
- [JDK 11+](https://openjdk.java.net/projects/jdk/11/)
- [Docker](https://www.docker.com/) or [Podman](https://podman.io)
- [Docker Compose](https://docs.docker.com/compose/) (optional but recommended)

### Download

To run this project locally, first clone it with Git:

```shell
git clone git@github.com:eyefloaters/console-api.git
cd console-api
```

### Install dependencies
Now you can install the required dependencies with Maven:

```shell
mvn install -DskipTests
```

### Run Instance API and Kafka cluster from CLI

The Managed Kafka Instance API needs a Apache Kafka cluster to connect to. There is a [docker-compose.yml](./docker-compose.yml) file with default
Kafka containers you can use to run the server against.

#### Kafka SASL_PLAINTEXT
Run the development server with a local Kafka cluster using a PLAINTEXT listener. Note, this command assumes the working directory is the project root.

```shell
make dev
```

This will start a Kafka cluster at localhost:9092

#### Kafka SASL_SSL

Run the development server with a local Kafka cluster using an SSL listener. Note, this command assumes the working directory is the project root.

```shell
make dev-tls
```

This will start a Kafka cluster at localhost:9093

### Using the Instance API

Once all steps above have been completed, you can interact with the Kafka Instance API. The server will start the following interfaces:
- REST API (`/api/v1/*`) on either [http://localhost:8080](http://localhost:8080)
- Metrics at [http://localhost:8080/metrics](http://localhost:8080/metrics)
- Health status at [http://localhost:8080/health](http://localhost:8080/health)
- OpenAPI at [http://localhost:8080/openapi](http://localhost:8080/openapi?format=json)
- Swagger UI at [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)

Interacting with the server requires credentials, most simply using HTTP Basic. With Basic enabled and the Kafka cluster started with `docker-compose`, the credentials
that may be utilized can be found in `kafka_server_jaas.conf`. For example, to obtain the list of available topics to the `admin` user:
```
curl -s -u admin:admin-secret http://localhost:8080/api/v1/topics
```

### Instance API Configuration

These options may be `export`ed prior to starting the server + Kafka broker as described above.

| Environment Variable | Description |
| -------------------- | ----------- |
| CORS_ALLOW_LIST_REGEX | Regular expression to match origins for CORS. An origin follows rfc6454#section-7 and is expected to have the format: `<scheme> "://" <hostname> [ ":" <port> ]` |
| KAFKA_ADMIN_BOOTSTRAP_SERVERS | A comma-separated list of host and port pairs that are the addresses of the Kafka brokers in a "bootstrap" Kafka cluster.   |
| KAFKA_ADMIN_BASIC_ENABLED | Enables HTTP Basic authentication. Request credentials are forwarded to the Kafka broker as SASL credentials. |
| KAFKA_ADMIN_OAUTH_ENABLED | Enables a third party application to obtain limited access to the Admin API. |
| KAFKA_ADMIN_OAUTH_TRUSTED_CERT | Certificate in PEM format used by the JWKS and token endpoints. Optional when the endpoints use self-signed or otherwise untrusted certificates. Only valid if OAuth and JWKS endpoint are enabled. |
| KAFKA_ADMIN_OAUTH_JWKS_ENDPOINT_URI | Endpoint serving JWKS to be use to verify JWT access tokens. *required when `KAFKA_ADMIN_OAUTH_ENABLED` is used* |
| KAFKA_ADMIN_OAUTH_VALID_ISSUER_URI | Optional issuer that, when provided, must match the issuer (`iss` claim) present in JWTs. Only valid if OAuth and JWKS endpoint are enabled. |
| KAFKA_ADMIN_OAUTH_TOKEN_ENDPOINT_URI | Optional token endpoint that will be published in the OpenAPI document describing the REST service. Only valid if OAuth and JWKS endpoint are enabled. |
| KAFKA_ADMIN_BROKER_TLS_ENABLED | Enables TLS for connections to the Kafka broker(s). |
| KAFKA_ADMIN_BROKER_TRUSTED_CERT | Certificate in PEM format used for Kafka broker trust. The value may be either a path to a file containing the certificate *or* text of the certificate. |
| KAFKA_ADMIN_TLS_CERT | TLS encryption certificate in PEM format. The value may be either a path to a file containing the certificate *or* text of the certificate. |
| KAFKA_ADMIN_TLS_KEY | TLS encryption private key in PEM format. The value may be either a path to a file containing the key *or* text of the key. *required when `KAFKA_ADMIN_TLS_CERT` is used* |
| KAFKA_ADMIN_TLS_VERSION | A comma-separated list of TLS versions to support for TLS/HTTPS endpoints. E.g. `TLSv1.3,TLSv1.2`. Default value if not specified is `TLSv1.3` |
| KAFKA_ADMIN_REPLICATION_FACTOR | Replication factor defines the number of copies of a topic in a Kafka cluster. |
| KAFKA_ADMIN_NUM_PARTITIONS_MAX | Maximum (inclusive) number of partitions that may be used for the creation of a new topic. |

## Updating OpenAPI file

```bash
 mvn -Popenapi-generate process-classes
```

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
