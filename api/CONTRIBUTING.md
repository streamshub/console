# Contributing to the API

## Before you start

Please check the project's [contributing guide](../CONTRIBUTING.md) first.

## Prerequisites

Ensure you have Kubernetes and Strimzi Cluster Operator installed on your system. Either [minikube](https://minikube.sigs.k8s.io/) or [OpenShift Local](https://developers.redhat.com/products/openshift-local) are good options.
One option to get started is to follow [Strimzi's Quick Starts](https://strimzi.io/quickstarts/).

You will also need a working installation of:

- Java (v21)
- Maven (v3.8)

### Coding Guidelines

 * We decided to disallow `@author` tags in the Javadoc: they are hard to maintain
 * Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.

### Continuous Integration

This project's CI is based on GitHub Actions, which means that everyone has the ability to automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process, so that the contributor gets immediate feedback, while at the same time keeping our CI fast and healthy for everyone.

### Tests and documentation are not optional

Do not forget to include or update tests in your pull requests and update any related documentation (reference documentation, javadoc...).

### Installing Checkstyle

Project uses checkstyle mvn plugin that is executed during `mvn validate` phase. Make sure to verify this
passes before pushing to Github.
