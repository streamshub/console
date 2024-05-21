# Console for Apache Kafka® on Kubernetes

This project is a web console designed to facilitate interactions with Apache Kafka® instances on Kubernetes, leveraging the [Strimzi](https://strimzi.io) Cluster Operator.
It is composed of two main parts:

- a REST API backend developed with Java and [Quarkus](https://quarkus.io/)
- a user interface (UI) built with [Next.js](https://nextjs.org/) and [PatternFly](https://patternfly.org)

#### Roadmap / Goals

The future goals of this project are to provide a user interface to interact with and manage additional data streaming components such as:

- [Apicurio Registry](https://www.apicur.io/registry/) for message serialization and de-serialization + validation
- [Kroxylicious](https://kroxylicious.io/)
- [Apache Flink](https://flink.apache.org/)

Contributions and discussions around use cases for these (and other relevant) components are both welcome and encouraged.

## Running the Application

The console application may either be run in a Kubernetes cluster or locally to try it out.

### Install to Kubernetes

Please refer to the [installation README](./install/README.md) file for detailed information about how to install the latest release of the console in a Kubernetes cluster.

### Run locally

Running the console locally requires the use of a remote or locally-running Kubernetes cluster that hosts the Strimzi Kafka operator
and any Apache Kafka® clusters that will be accessed from the console. To get started, you will need to provide a console configuration
file and credentials to connect to the Kubernetes cluster where Strimzi and Kafka are available.

1. Using the [console-config-example.yaml](./console-config-example.yaml) file as an example, create your own configuration
   in a file `console-config.yaml` in the repository root. The `compose.yaml` file expects this location to be used and
   and difference in name or location requires an adjustment to the compose file.

2. Install the prerequisite software into the Kubernetes cluster. This step assumes none have yet been installed.
   ```shell
   ./install/000-install-dependency-operators.sh <your namespace>
   ./install/001-deploy-prometheus.sh <your namespace> <your cluster base domain>
   ./install/002-deploy-console-kafka.sh <your namespace> <your cluster base domain>
   ```
   Note that the Prometheus instance will be available at `http://console-prometheus.<your cluster base domain>` when this step
   completes.

3. Provide the Prometheus endpoint, the API server endpoint, and the service account token that you would like to use to connect to the Kubernetes cluster. These may be placed in a `compose.env` file that will be detected when starting the console.
   ```
   CONSOLE_API_SERVICE_ACCOUNT_TOKEN=<TOKEN>
   CONSOLE_API_KUBERNETES_API_SERVER_URL=https://my-kubernetes-api.example.com:6443
   CONSOLE_METRICS_PROMETHEUS_URL=http://console-prometheus.<your cluster base domain>
   ```
   The service account token may be obtain using the `kubectl create token` command. For example, to create a service account
   named "console-server" (from [console-server.serviceaccount.yaml](./install/resources/console/console-server.serviceaccount.yaml)
   with the correct permissions and a token that expires in 1 year ([yq](https://github.com/mikefarah/yq/releases) required):
   ```shell
   export NAMESPACE=<service account namespace>
   kubectl apply -n ${NAMESPACE} -f ./install/resources/console/console-server.clusterrole.yaml
   kubectl apply -n ${NAMESPACE} -f ./install/resources/console/console-server.serviceaccount.yaml
   yq '.subjects[0].namespace = strenv(NAMESPACE)' ./install/resources/console/console-server.clusterrolebinding.yaml | kubectl apply -n ${NAMESPACE} -f -
   kubectl create token console-server -n ${NAMESPACE} --duration=$((365*24))h
   ```

4. By default, the provided configuration will use the latest console release container images. If you would like to
   build your own images with changes you've made locally, you may also set the `CONSOLE_API_IMAGE` and `CONSOLE_UI_IMAGE`
   in your `compose.env` and build them with `make container-images`

5. Start the environment with `make compose-up`.

6. When finished with the local console process, you may run `make compose-down` to clean up.

## Contributing

We welcome contributions of all forms. Please see the [CONTRIBUTING](./CONTRIBUTING.md) file for how to get started.
Join us in enhancing the capabilities of this console for Apache Kafka® on Kubernetes.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
