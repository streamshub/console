# Console Installation

This directory contains several resources that may be used directly or as examples for deploying the
console into a Kubernetes or OpenShift cluster. The scripts contained here may be run using the bash
shell and require that the `kubectl` and [yq](#ref-1) command line utilities are available
on the `PATH`.

## Prerequisites

### Strimzi & Prometheus

The console requires that the Strimzi Kafka Operator is installed and available in the cluster before
deployment. Strimzi may be installed either using Operator Lifecycle Manager (OLM, preferred) or directly
using Kubernetes resources provided by the Strimzi project.

Prometheus must also be installed and configured to scrape metrics from Kubernetes and Kafka clusters. The
Prometheus instance must be accessible to the console application using HTTP or HTTPS. If a Prometheus instance
is not available, the cluster metrics graphs on the Kafka cluster overview screens will be unavailable.

Users who do not previously have Strimzi and Promethus installed may use the `install-community-operators.sh`
and `deploy-prometheus.sh` scripts to bootstrap the environment. The scripts will install the community-supported
versions of the two operators using OLM and deploy a Prometheus instance configured to scrape metrics from any
Kafka clusters deployed by Strimzi within the cluster.

```shell
install-community-operators.sh ${TARGET_NAMESPACE}
deploy-prometheus.sh ${TARGET_NAMESPACE}
```

### Apache Kafka Cluster

Once the two prerequisite components have been installed

## References

<a id="ref-1"></a>`yq` [releases](https://github.com/mikefarah/yq/releases)
