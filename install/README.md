# Console Installation

This directory contains several resources that may be used directly or as examples for deploying the
console into a Kubernetes or OpenShift cluster. The scripts contained here may be run using the bash
shell and require that the `kubectl` (or `oc` for OpenShift) and `yq`[1] command line utilities
are available on the `PATH`.

## Prerequisites

### 0. Minikube Setup (optional)

If using minikube, be sure to enable ingress with TLS passthrough and install OLM.

```shell
minikube start

# Enable ingress
minikube addons enable ingress

# Enable TLS passthrough on the ingress deployment
kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
 --type='json' \
 -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'

# Install OLM
curl -sL https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.28.0/install.sh | bash -s "v0.28.0"
```

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
```

If not using the operator to create a console instance, a Prometheus instance must be deployed next.

```shell
001-deploy-prometheus.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN}
```

Note, in this example the operators installed will only watch the `${TARGET_NAMESPACE}` namespace. This must
be taken into account when deploying the Apache Kafka cluster below where the namespace must be provided to the script.

### 2. Apache Kafka Cluster

Once the operators have been installed, the demo Kafka cluster may be created using the
`002-deploy-console-kafka.sh` script. This script will create a Strimzi `Kafka` custom resource as well as a
`KafkaUser` custom resource for a user to access the cluster. Additionally, the Kafka cluster will be configured via
a ConfigMap to export metrics in the way expected by the Prometheus instance that will be created along with the console
(if using the operator) or the instance created earlier with `001-deploy-prometheus.sh`.

```shell
002-deploy-console-kafka.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN}
```

By default, the Kafka cluster will be created in KRaft mode (Zookeeper-less). If you would like to create a cluster
that uses ZooKeeper, an additional third argument with the value `zk` may be given. The value may also be `kraft` to
explicitly request a KRaft cluster.

```shell
002-deploy-console-kafka.sh ${TARGET_NAMESPACE} ${CLUSTER_DOMAIN} zk
```

#### Kafka Authorization

In order to allow the necessary access for the console to function, a minimum level of authorization must be configured
for the principal in use for each Kafka cluster connection. While the definition of the permissions may vary depending
on the authorization framework in use (e.g. ACLs, Keycloak Authorization, OPA, or custom) the minimum required in terms
of ACL types are:

1. `DESCRIBE`, `DESCRIBE_CONFIGS` for the `CLUSTER` resource
1. `READ`, `DESCRIBE`, `DESCRIBE_CONFIGS` for all `TOPIC` resources
1. `READ`, `DESCRIBE` for all `GROUP` resources

### 3. Console Operator

The next step will install the StreamsHub console operator into the namespace provided. This step is optional
if, for example, you will be running either the console or the operator on your local machine in development mode.

```shell
003-install-console-operator.sh ${CONSOLE_NAMESPACE}
```

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
      credentials:
        kafkaUser:
          name: console-kafka-user1             # Name of the `KafkaUser` resource used to connect to Kafka
' | kubectl apply -n ${CONSOLE_NAMESPACE} -f -
```

## References

[1] yq [releases](https://github.com/mikefarah/yq/releases)
