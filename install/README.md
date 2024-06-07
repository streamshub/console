# Console Installation

This directory contains several resources that may be used directly or as examples for deploying the
console into a Kubernetes or OpenShift cluster. The scripts contained here may be run using the bash
shell and require that the `kubectl` (or `oc` for OpenShift) and `yq`[1] command line utilities
are available on the `PATH`.

## Prerequisites

### 1. Strimzi & Prometheus

The console requires that the Strimzi Kafka Operator is installed and available in the cluster before
deployment. Strimzi may be installed either using Operator Lifecycle Manager (OLM, preferred) or directly
using Kubernetes resources provided by the Strimzi project.

Prometheus must also be installed and configured to scrape metrics from Kubernetes and Kafka clusters. The
Prometheus instance must be accessible to the console application using HTTP or HTTPS. If a Prometheus instance
is not available, the cluster metrics graphs on the Kafka cluster overview screens will be unavailable.

Users who do not previously have Strimzi and Promethus installed may use the `000-install-dependency-operators.sh`
and `001-deploy-prometheus.sh` scripts to bootstrap the environment. The scripts will install either the community-supported
or commercially supported (i.e. streams for Apache Kafka) version of the two operators using OLM and deploy a Prometheus instance
configured to scrape metrics from Kafka clusters deployed by Strimzi within the cluster.

```shell
000-install-dependency-operators.sh ${TARGET_NAMESPACE}
001-deploy-prometheus.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN}
```

### 2. Apache Kafka Cluster

Once the two prerequisite components have been installed, the demo Kafka cluster may be created using the
`002-deploy-console-kafka.sh` script. This script will create a Strimzi `Kafka` custom resource as well as a
`KafkaUser` custom resource for a user to access the cluster. Additionally, the Kafka cluster will be configured via
a ConfigMap to export metrics in the way expected by the Prometheus instance created earlier.

```shell
002-deploy-console-kafka.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN}
```

By default, the Kafka cluster will be created in KRaft mode (Zookeeper-less). If you would like to create a cluster
that uses ZooKeeper, an additional third argument with the value `zk` may be given. The value may also be `kraft` to
explicitly request a KRaft cluster.

```shell
002-deploy-console-kafka.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN} zk
```

### Authorization

In order to allow the necessary access for the console to function, a minimum level of authorization must be configured
for the principal in use for each Kafka cluster connection. While the definition of the permissions may vary depending
on the authorization framework in use (e.g. ACLs, Keycloak Authorization, OPA, or custom) the minimum required in terms
of ACL types are:

1. `DESCRIBE`, `DESCRIBE_CONFIGS` for the `CLUSTER` resource
1. `READ`, `DESCRIBE`, `DESCRIBE_CONFIGS` for all `TOPIC` resources
1. `READ`, `DESCRIBE` for all `GROUP` resources

## 3. Installation

With the prerequisites met, the console can be deployed using the `003-install-console.sh` script. This script will
create the role, role binding, service account, services, and ingress (or route in OpenShift) necessary to run the console.
Finally, the console deployment is applied to the Kubernetes/OpenShift cluster. A link to access the application will
be printed to the script's output if no errors are encountered.

The configurations used by the console to connect to Kafka may be customized by providing your own configuration.
See [console-config-example.yaml](../console-config-example.yaml) in the repository root for an example. The path to
the customized configuration must be provided as the third argument of `003-install-console.sh`

```shell
003-install-console.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN} ${CONSOLE_CONFIG}
```

## References

[1] yq [releases](https://github.com/mikefarah/yq/releases)
