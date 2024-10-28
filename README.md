# StreamsHub Console for Apache Kafka<sup>®</sup>
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=streamshub_console&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=streamshub_console) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=streamshub_console&metric=coverage)](https://sonarcloud.io/summary/new_code?id=streamshub_console)
StreamsHub Console is a web application designed to facilitate interactions with Apache Kafka<sup>®</sup> instances, optionally leveraging the [Strimzi](https://strimzi.io) Cluster Operator for Kafka<sup>®</sup> instances running on Kubernetes.
It is composed of three main parts:
- a [REST API](./api) backend developed with Java and [Quarkus](https://quarkus.io/)
- a [user interface (UI)](./ui) built with [Next.js](https://nextjs.org/) and [PatternFly](https://patternfly.org)
- a Kubernetes [operator](./operator) developed with Java and [Quarkus](https://quarkus.io/)

## Roadmap / Goals

The future goals of this project are to provide a user interface to interact with and manage additional data streaming components such as:
- [Apicurio Registry](https://www.apicur.io/registry/) for message serialization and de-serialization + validation
- [Kroxylicious](https://kroxylicious.io/) for introducing additional behaviors to Kafka-based systems
- [Apache Flink](https://flink.apache.org/) for processing real-time data streams and batch data sets

Contributions and discussions around use cases for these (and other relevant) components are both welcome and encouraged.

## Deployment
Deploy the console using one of the following methods:

- Through its dedicated operator using the Operator Lifecycle Manager (OLM)
- Using the operator with plain Kubernetes resources
- Directly with Kubernetes resources, without the operator

Note, if you are using [minikube](https://minikube.sigs.k8s.io/) with the `ingress` addon as your Kubernetes cluster, SSL pass-through must be enabled on the nginx controller:
```shell
# Enable TLS passthrough on the ingress deployment
kubectl patch deployment -n ingress-nginx ingress-nginx-controller \
 --type='json' \
 -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
```

### Prerequisites
#### Apache Kafka®
The instructions below assume an existing Apache Kafka<sup>®</sup> cluster is available to use from the console. We recommend using [Strimzi](https://strimzi.io) to create and manage your Apache Kafka<sup>®</sup> clusters - plus the console provides additional features and insights for Strimzi Apache Kafka<sup>®</sup> clusters.

If you already have Strimzi installed but would like to create an Apache Kafka<sup>®</sup> cluster for use with the console, example deployment resources are available to get started.  The resources create an Apache Kafka<sup>®</sup> cluster in KRaft mode with SCRAM-SHA-512 authentication, a Strimzi `KafkaNodePool` resource to manage the cluster nodes, and a Strimzi `KafkaUser` resource that may be used to connect to the cluster.

Modify the `CLUSTER_DOMAIN` to match the base domain of your Kubernetes cluster (used for ingress configuration), use either `route` (OpenShift) or `ingress` (vanilla Kubernetes) for `LISTENER_TYPE`, and set `NAMESPACE` to be the namespace where the Apache Kafka<sup>®</sup> cluster will be created.
```shell
export CLUSTER_DOMAIN=apps-crc.testing
export NAMESPACE=kafka
export LISTENER_TYPE=route
cat examples/kafka/*.yaml | envsubst | kubectl apply -n ${NAMESPACE} -f -
```
##### Kafka Authorization
To ensure the console has the necessary access to function, a minimum level of authorization must be configured for the principal used in each Kafka cluster connection. The specific permissions may vary based on the authorization framework in use, such as ACLs, Keycloak authorization, OPA, or a custom solution. However, the minimum ACL types required are as follows:
1. `DESCRIBE`, `DESCRIBE_CONFIGS` for the `CLUSTER` resource
2. `READ`, `DESCRIBE`, `DESCRIBE_CONFIGS` for all `TOPIC` resources
3. `READ`, `DESCRIBE` for all `GROUP` resources

#### Prometheus
Prometheus is an optional dependency of the console if cluster metrics are to be displayed. The operator currently installs a private Prometheus instance for each `Console` instance. However, when installing a single console deployment, Prometheus must be either installed separately or provided via a URL reference. This will be addressed below in the section dealing with creating a console via a `Deployment`.

### Deploy the operator with OLM
The preferred way to deploy the console is using the Operator Lifecycle Manager, or OLM. The sample install files in `install/operator-olm` will install the operator with cluster-wide scope. This means that `Console` instances may be created in any namespace. If you wish to limit the scope of the operator, the `OperatorGroup` resource may be modified to specify only the namespace that should be watched by the operator.

This example will create the operator's OLM resources in the `default` namespace. Modify the `NAMESPACE` variable according to your needs.
```shell
export NAMESPACE=default
cat install/operator-olm/*.yaml | envsubst | kubectl apply -n ${NAMESPACE} -f -
```
#### Console Custom Resource Example
Once the operator is ready, you may then create a `Console` resource in the namespace where the console should be deployed. This example `Console` is based on the example Apache Kafka<sup>®</sup> cluster deployed above in the [prerequisites section](#prerequisites). Also see [examples/console/010-Console-example.yaml](examples/console/010-Console-example.yaml).
```yaml
apiVersion: console.streamshub.github.com/v1alpha1
kind: Console
metadata:
  name: example
spec:
  hostname: example-console.apps-crc.testing # Hostname where the console will be accessed via HTTPS
  kafkaClusters:
    - name: console-kafka             # Name of the `Kafka` CR representing the cluster
      namespace: kafka                # Namespace of the `Kafka` CR representing the cluster
      listener: secure                # Listener on the `Kafka` CR to connect from the console
      properties:
        values: []                    # Array of name/value for properties to be used for connections
                                      # made to this cluster
        valuesFrom: []                # Array of references to ConfigMaps or Secrets with properties
                                      # to be used for connections made to this cluster
      credentials:
        kafkaUser:
          name: console-kafka-user1   # Name of the `KafkaUser` resource used to connect to Kafka
                                      # This is optional if properties are used to configure the user
```

### Deploy the operator directly
Deploying the operator without the use of OLM requires applying the component Kubernetes resources for the operator directly. These resources are bundled and attached to each StreamsHub Console release. The latest release can be found [here](https://github.com/streamshub/console/releases/latest). The resource file is named `console-operator-x.y.z.yaml` where `x.y.z` is the released version.

This example will create the operator's resources in the `default` namespace. Modify the `NAMESPACE` variable according to your needs and set `VERSION` to the [latest release](https://github.com/streamshub/console/releases/latest).
```
export NAMESPACE=default
export VERSION=0.3.3
curl -sL https://github.com/streamshub/console/releases/download/${VERSION}/console-operator-${VERSION}.yaml \
  | envsubst \
  | kubectl apply -n ${NAMESPACE} -f -
```
Note: if you are not using the Prometheus operator you may see an error about a missing `ServiceMonitor` custom resource type. This error may be ignored.

With the operator resources created, you may create a `Console` resource like the one shown in [Console Custom Resource Example](#console-custom-resource-example).

## Running locally

Running the console locally requires configuration of any Apache Kafka<sup>®</sup> clusters that will be accessed from the console and (optionally) the use of a Kubernetes cluster that hosts the Strimzi Kafka operator. To get started, you will need to provide a console configuration file and (optionally) credentials to connect to the Kubernetes cluster where Strimzi is operating.

1. Using the [console-config-example.yaml](./console-config-example.yaml) file as an example, create your own configuration
   in a file `console-config.yaml` in the repository root. The `compose.yaml` file expects this location to be used and
   any difference in name or location requires an adjustment to the compose file.
2. Install the prerequisite software into the Kubernetes cluster.
    * Install the [Strimzi operator](https://strimzi.io/docs/operators/latest/deploying#con-strimzi-installation-methods_str)
    * Install the [Prometheus operator](https://github.com/prometheus-operator/prometheus-operator/blob/main/Documentation/user-guides/getting-started.md) and create a `Prometheus` instance (_optional_, only if you want to see metrics in the console)
    * Create an Apache Kafka<sup>®</sup> cluster. See the [example above](#apache-kafka) This step is only required if you do not already have an existing cluster you would like to use with the console.
3. (_Skip this step if you are not using Kubernetes and Prometheus_) Provide the Prometheus endpoint, the API server endpoint, and the service account token that you would like to use to connect to the Kubernetes cluster. These may be placed in a `compose.env` file that will be detected when starting the console.
   ```
   CONSOLE_API_SERVICE_ACCOUNT_TOKEN=<TOKEN>
   CONSOLE_API_KUBERNETES_API_SERVER_URL=https://my-kubernetes-api.example.com:6443
   CONSOLE_METRICS_PROMETHEUS_URL=http://console-prometheus.<your cluster base domain>
   ```
   The service account token may be obtained using the `kubectl create token` command. For example, to create a service account named "console-server" with the correct permissions and a token that expires in 1 year ([yq](https://github.com/mikefarah/yq/releases) required):
   ```shell
   export NAMESPACE=<service account namespace>
   kubectl apply -n ${NAMESPACE} -f ./install/console/010-ServiceAccount-console-server.yaml
   kubectl apply -n ${NAMESPACE} -f ./install/console/020-ClusterRole-console-server.yaml
   cat ./install/console/030-ClusterRoleBinding-console-server.yaml | envsubst | kubectl apply -n ${NAMESPACE} -f -
   kubectl create token console-server -n ${NAMESPACE} --duration=$((365*24))h
   ```

4. By default, the provided configuration will use the latest console release container images. If you would like to
   build your own images with changes you've made locally, you may also set the `CONSOLE_API_IMAGE` and `CONSOLE_UI_IMAGE`
   in your `compose.env` and build them with `make container-images`

5. Start the environment with `make compose-up`.

6. When finished with the local console process, you may run `make compose-down` to clean up.

## Contributing

We welcome contributions of all forms. Please see the [CONTRIBUTING](./CONTRIBUTING.md) file for how to get started.
Join us in enhancing the capabilities of this console for Apache Kafka<sup>®</sup> on Kubernetes.

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

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

