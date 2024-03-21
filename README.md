# Console for Apache Kafka™ on Kubernetes

This project is a web console designed to facilitate interactions with Apache Kafka™ instances on Kubernetes, leveraging the [Strimzi](https://strimzi.io) Cluster Operator. 
It is composed of two main parts: 

- a REST API backend developed with Java Quarkus
- a user interface (UI) built with Next.js and  [PatternFly](https://patternfly.org)

### Getting started

#### API

Ensure you have Kubernetes and Strimzi Cluster Operator installed on your system.

```bash
cd api
```

Create a `.env` file containing the details about how the API should connect to the Kafka cluster.
For a Kafka cluster installed using [Strimzi's Quick Starts](https://strimzi.io/quickstarts/) it should look like this:

```.dotenv
CONSOLE_KAFKA_MY_CLUSTER=kafka/my-cluster
CONSOLE_KAFKA_MY_CLUSTER_BOOTSTRAP_SERVERS=my-cluster-kafka-bootstrap:9092
```

Then run the application. 

```bash
./mvn quarkus:dev -DskipTests
```

This should result in Quarkus starting up, with the REST APIs available on localhost port 8080:

* [API documentation](http://localhost:8080/swagger-ui)

#### UI

```bash
cd ui
```

Create a `.env` file containing the details about where to find the API server, and some additional config.

```.dotenv
BACKEND_URL=http://localhost:8080
CONSOLE_METRICS_PROMETHEUS_URL=http://localhost:9090
NEXTAUTH_SECRET=abcdefghijklmnopqrstuvwxyz1234567890=
LOG_LEVEL=info
```

[!WARNING]
Please generate a valid and secure value for `NEXTAUTH_SECRET`. We suggest running `openssl rand -base64 32` to get started.

Then run the application.

```bash
npm install
npm run dev
```

This will start up the UI in development mode, hosted on port 3000 of your localhost:

## [User Interface](http://localhost:3000)

For more information on the UI, see the UI module's [README.md](ui/README.md).

## Contributing

We welcome contributions of all forms. Please see the CONTRIBUTING.md file for how to get started. Join us in enhancing the capabilities of this console for Apache Kafka™ on Kubernetes.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.