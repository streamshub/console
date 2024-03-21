# Contributing to the API

## Before you start

Please check the project's [contributing guide](../CONTRIBUTING.md) first.

## Prerequisites

Ensure you have Kubernetes and Strimzi Cluster Operator installed on your system.
One option to get started is to follow [Strimzi's Quick Starts](https://strimzi.io/quickstarts/).

You will also need a working installation of:

- Java (v17)
- Maven (v3.8)

### Coding Guidelines

 * We decided to disallow `@author` tags in the Javadoc: they are hard to maintain
 * Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.

### Continuous Integration

 kafka-admin-api CI is based on GitHub Actions, which means that everyone has the ability to automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process, so that the contributor gets immediate feedback, while at the same time keeping our CI fast and healthy for everyone.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests.
Also don't forget the documentation (reference documentation, javadoc...).

### Installing Checkstyle

Project uses checkstyle mvn plugin that is executed during `mvn validate` pase.
Please follow your ide setup for checkstyle. For example for intelij:

https://plugins.jetbrains.com/plugin/1065-checkstyle-idea

## Regenerating OpenAPI file

PRs that make changes in the API should update openapi file by executing:

```
mvn -Popenapi-generate process-classes
```

Please commit generated files along with the PR for review.

### Interacting with local kafka

1. Creating topic

```
kafka-topics.sh --create --bootstrap-server localhost:9092  --partitions=3 --replication-factor=1 --topic test --command-config ./hack/binscripts.properties
```

2. Produce messages using kcat
```
kcat -b localhost:9092 -F ./hack/kcat.properties -P -t test
```


4. Consume messages
```
 kcat -b localhost:9092 -F ./hack/kcat.properties  -C -t test
```

6. Interact with the API to view results
`
curl -s -u admin:admin-secret http://localhost:8080/api/v1/consumer-groups | jq
`

