# Testing console-api

## Running tests

### Requirements
Docker environment with at least 2 GB of RAM

### Running tests
Execution
```shell
mvn clean verify
```

Single class/test execution
```shell
mvn clean verify -Dit.test=TopicsResourceIT
mvn clean verify -Dit.test=TopicsResourceIT#testListTopicsAfterCreation
```

### Running tests in an IDE
The values for system properties `java.util.logging.manager`, `keycloak.image` and `strimzi-kafka.tag` must be configured if your IDE does not read them from the `pom.xml`.
See `pom.xml` for the values used by `mvn`.

### Remote Debugging
The tests will run with remote debugging enabled on the host's port configured via the `debug` system property (e.g. `-Ddebug=5005`).
You can attach your IDE to the remote debug port by first setting a breakpoint in the test method you would like to debug, then attach to the remote debugger on the configured port once the test method breakpoint is hit.
