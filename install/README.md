# Console Installation

This directory contains several resources that may be used directly or as examples for deploying the
console into a Kubernetes or OpenShift cluster. The scripts contained here may be run using the bash
shell and require that the `kubectl` (or `oc` for OpenShift) and `yq`[1] command line utilities
are available on the `PATH`.

## Prerequisites

### 1. Strimzi

The console requires that the Strimzi Kafka Operator is installed and available in the cluster before
deployment. Strimzi may be installed either using Operator Lifecycle Manager (OLM, preferred) or directly
using Kubernetes resources provided by the Strimzi project.

Users who do not previously have Strimzi installed may use the `000-install-strimzi-operator.sh` script to bootstrap the
environment. The script will install either the community-supported or commercially supported (i.e. streams for Apache Kafka)
version of the operator using OLM.

```shell
000-install-strimzi-operator.sh ${STRIMZI_NAMESPACE}
```

Note, in this example the Strimzi operator installed will only watch the `${STRIMZI_NAMESPACE}` namespace. This must
be taken into account when deploying the Apache Kafka cluster below where the namespace must be provided to the script.

### 2. Console Operator

The next step will install the StreamsHub console operator into the namespace provided.

```shell
001-install-console-operator.sh ${CONSOLE_NAMESPACE}
```

### 3. Apache Kafka Cluster

Once the operators have been installed, the demo Kafka cluster may be created using the
`002-deploy-console-kafka.sh` script. This script will create a Strimzi `Kafka` custom resource as well as a
`KafkaUser` custom resource for a user to access the cluster. Additionally, the Kafka cluster will be configured via
a ConfigMap to export metrics in the way expected by the Prometheus instance that will be created along with the console.

```shell
002-deploy-console-kafka.sh ${STRIMZI_NAMESPACE} ${CLUSTER_DOMAIN}
```

By default, the Kafka cluster will be created in KRaft mode (Zookeeper-less). If you would like to create a cluster
that uses ZooKeeper, an additional third argument with the value `zk` may be given. The value may also be `kraft` to
explicitly request a KRaft cluster.

```shell
002-deploy-console-kafka.sh ${STRIMZI_NAMESPACE} ${CLUSTER_DOMAIN} zk
```

#### Kafka Authorization

In order to allow the necessary access for the console to function, a minimum level of authorization must be configured
for the principal in use for each Kafka cluster connection. While the definition of the permissions may vary depending
on the authorization framework in use (e.g. ACLs, Keycloak Authorization, OPA, or custom) the minimum required in terms
of ACL types are:

1. `DESCRIBE`, `DESCRIBE_CONFIGS` for the `CLUSTER` resource
1. `READ`, `DESCRIBE`, `DESCRIBE_CONFIGS` for all `TOPIC` resources
1. `READ`, `DESCRIBE` for all `GROUP` resources

## Creating a Console Instance

With the prerequisites met, a `Console` custom resource instance can be created using `kubectl`. This resource must
contain a reference to the `Kafka` and `KafkaUser` resources created above by `002-deploy-console-kafka.sh`.

```shell
echo 'apiVersion: console.streamshub.github.com/v1alpha1
kind: Console
metadata:
  name: example
spec:
  hostname: example-console.'${CLUSTER_DOMAIN}' # Hostname where the console will be accessed via HTTPS
  kafkaClusters:
    - name: console-kafka                       # Name of the `Kafka` CR representing the cluster
      namespace: '${STRIMZI_NAMESPACE}'         # Namespace of the `Kafka` CR representing the cluster
      listener: secure                          # Listener on the `Kafka` CR to connect from the console
      kafkaUserName: console-kafka-user1        # Name of the `KafkaUser` resource used to connect to Kafka
' | kubectl apply -n ${CONSOLE_NAMESPACE} -f -
```

## References

[1] yq [releases](https://github.com/mikefarah/yq/releases)
