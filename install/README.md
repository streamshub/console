# Console Installation

This directory contains several resources that may be used directly or as examples for deploying the
console into a Kubernetes or OpenShift cluster. The scripts contained here may be run using the bash
shell and require that the `kubectl` (or `oc` for OpenShift) and `yq`[1] command line utilities
are available on the `PATH`.

## Prerequisites

### Strimzi & Prometheus

The console requires that the Strimzi Kafka Operator is installed and available in the cluster before
deployment. Strimzi may be installed either using Operator Lifecycle Manager (OLM, preferred) or directly
using Kubernetes resources provided by the Strimzi project.

Prometheus must also be installed and configured to scrape metrics from Kubernetes and Kafka clusters. The
Prometheus instance must be accessible to the console application using HTTP or HTTPS. If a Prometheus instance
is not available, the cluster metrics graphs on the Kafka cluster overview screens will be unavailable.

Users who do not previously have Strimzi and Promethus installed may use the `000-install-dependency-operators.sh`
and `001-deploy-prometheus.sh` scripts to bootstrap the environment. The scripts will install either the community-supported
or commercially supported (i.e. AMQ Streams) version of the two operators using OLM and deploy a Prometheus instance
configured to scrape metrics from any Kafka clusters deployed by Strimzi within the cluster.

```shell
000-install-dependency-operators.sh ${TARGET_NAMESPACE}
001-deploy-prometheus.sh ${TARGET_NAMESPACE}
```

### Apache Kafka Cluster

Once the two prerequisite components have been installed, the demo Kafka cluster may be created using the
`002-deploy-demo-cluster.sh` script. This script will create a Strimzi `Kafka` custom resource as well as a
`KafkaUser` custom resource for a user to access the cluster. Additionally, the Kafka cluster will be configured via
a ConfigMap to export metrics in the way expected by the Prometheus instance created earlier.

### Authorization

In order to allow the necessary access for the console to function, a minimum level of authorization must be configured
for the principal in use for each Kafka cluster connection. While the definition of the permissions may vary depending
on the authorization framework in use (e.g. ACLs, Keycloak Authorization, OPA, or custom) the minimum required in terms
of ACL types are:

1. `DESCRIBE`, `DESCRIBE_CONFIGS` for the `CLUSTER` resource
1. `READ`, `DESCRIBE`, `DESCRIBE_CONFIGS` for all `TOPIC` resources
1. `READ`, `DESCRIBE` for all `GROUP` resources

## Installation

With the prerequisites met, the console can be deployed using the `003-install-console.sh` script. This script will
create the role, role binding, service account, services, and ingress (or route in OpenShift) necessary to run the console.
Finally, the console deployment is applied to the Kubernetes/OpenShift cluster. A link to access the application will
be printed to the script's output if no errors are encountered.

The configurations used by the console to connect to Kafka may be customized by altering the environment variables
for the `console-api` container in `resources/console/console.deployment.yaml`. The format used for the variables
is as follows.

Configurations that apply to all Kafka connections should use the format `KAFKA_CONFIG_WITH_UNDERSCORES`. For example,
if all clusters are configured to use `SASL_SSL` for the Kafka `security.protocol` property, you may set env
`KAFKA_SECURITY_PROTOCOL` to `SASL_SSL`.

Each individual cluster must be configured with a variable like `CONSOLE_KAFKA_CLUSTER1` where `CLUSTER1` is a unique
name or identifier for each cluster and the value of the env is the `${namespace}/${name}` of the `Kafka` CR that
represents the cluster.

Configurations that apply to an individual Kafka connection should use the format `CONSOLE_KAFKA_CLUSTER1_CONFIG_WITH_UNDERSCORES`.
Using the example above, if you would like to configure one cluster to use `SASL_SSL` for the Kafka `security.protocol` property,
the following env settings would be needed:

```yaml
- name: CONSOLE_KAFKA_EXAMPLE
  value: example-ns/demo-cluster
- name: CONSOLE_KAFKA_EXAMPLE_SECURITY_PROTOCOL
  value: SASL_SSL
- name: CONSOLE_KAFKA_EXAMPLE_BOOTSTRAP_SERVERS
  value: bootstrap.demo-cluster.example.com:443
```

As always, configuration properties that contain sensitive information may be mounted from a `Secret`. For example, to
set the `sasl.jaas.config` property, you could use an env entry such as the following.

```yaml
- name: CONSOLE_KAFKA_EXAMPLE_SASL_JAAS_CONFIG
  valueFrom:
    secretKeyRef:
      name: demo-cluster-user1
      key: sasl.jaas.config
```

## References

[1] yq [releases](https://github.com/mikefarah/yq/releases)
