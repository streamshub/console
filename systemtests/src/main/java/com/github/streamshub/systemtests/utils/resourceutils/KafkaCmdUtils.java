package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.strimzi.api.kafka.model.connect.KafkaConnectResources;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaCmdUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaCmdUtils.class);

    private static final String CLIENTS_CONFIG_FILE_PATH = "/tmp/client.properties";

    private KafkaCmdUtils() {}

    /**
     * Retrieves the timestamp (CreateTime) of a Kafka message at a specific offset within a given topic and partition.
     * <p>
     * This method:
     * <ul>
     *     <li>Creates a {@code client.properties} file inside the specified pod using the given client configuration.</li>
     *     <li>Executes {@code kafka-console-consumer.sh} to consume a message from a given offset.</li>
     *     <li>Parses and returns the {@code CreateTime} timestamp of the consumed message using {@code awk}.</li>
     * </ul>
     *
     * @param namespaceName   the namespace of the pod executing the command
     * @param kafkaName       the name of the Kafka cluster
     * @param podName         the name of the pod in which to run the Kafka consumer command
     * @param topicName       the Kafka topic to consume from
     * @param clientsConfig   the content of the Kafka client configuration (e.g., security configs)
     * @param offset          the offset from which to consume the message
     * @param partition       the partition to consume from
     * @param maxMessages     the number of messages to consume (typically set to 1)
     * @return the timestamp of the message at the given offset, or an empty string if not found
     */
    public static String getConsumerOffsetTimestampFromOffset(String namespaceName, String kafkaName, String podName, String topicName, String clientsConfig, String offset, int partition, int maxMessages) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);
        insertClientProperties(namespaceName, podName, clientsConfig);

        String getOffsetCommand = String.format("./bin/kafka-console-consumer.sh --bootstrap-server=%s --consumer.config=%s --topic=%s --offset=%s --partition=%d --max-messages=%d --property=print.timestamp=true 2>/dev/null" +
                                                " | awk -F'[:\\t]' '/CreateTime:/ {print $2}'", bootstrapServer, CLIENTS_CONFIG_FILE_PATH, topicName, offset, partition, maxMessages);

        LOGGER.debug("Execute get offset command");
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", getOffsetCommand).out().trim();
        LOGGER.debug("Execution resulted in => [{}]", output);
        return output;
    }

    /**
     * Reassigns a Kafka topic partition to a different broker using the Kafka reassignment tool inside a Kafka broker pod.
     *
     * <p>This method creates the necessary client properties and reassignment JSON files inside the target pod, executes
     * the reassignment, verifies its success, and cleans up temporary files afterwards.</p>
     *
     * <p>It is primarily used to simulate partition placement on a specific broker, which is useful when testing topic availability
     * scenarios such as deleting the broker holding the only replica.</p>
     *
     * <p>The following steps are performed inside the given pod:</p>
     * <ol>
     *   <li>Create a `client.properties` file using the provided SCRAM-SHA client configuration.</li>
     *   <li>Create a reassignment JSON file assigning the specified topic's partition to the target broker ID.</li>
     *   <li>Execute the Kafka partition reassignment using `kafka-reassign-partitions.sh`.</li>
     *   <li>Verify the reassignment.</li>
     *   <li>Remove the created files to clean up the environment.</li>
     * </ol>
     *
     * @param namespaceName the namespace where the Kafka cluster and pod are running
     * @param kafkaName the name of the Kafka cluster (used to derive the bootstrap address)
     * @param podName the name of the Kafka broker pod in which commands are executed
     * @param topicName the name of the Kafka topic whose partition is being reassigned
     * @param newBrokerId the broker ID to which the partition will be reassigned
     * @param clients the {@link KafkaClients} instance containing configuration used for authentication
     */
    public static void reassignTopicPartitionToAnotherBroker(String namespaceName, String kafkaName, String podName, String topicName, int newBrokerId, KafkaClients clients) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);

        LOGGER.debug("Reassigning KafkaTopic {} ", topicName);
        String reassignJsonPath = String.format("/tmp/reassign-%s.json", topicName);

        insertClientProperties(namespaceName, podName, clients.getAdditionalConfig());

        String reassignJson = String.format("echo '{\"version\":1,\"partitions\":[{\"topic\":\"%s\",\"partition\":0,\"replicas\":[%d],\"log_dirs\":[\"any\"]}]}' >> %s", topicName, newBrokerId, reassignJsonPath);
        LOGGER.debug("Insert reassign json => [{}]", reassignJson);
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", reassignJson).out().trim();
        LOGGER.debug("Inserting resulted in => [{}]", output);

        String reassignCommand = String.format("./bin/kafka-reassign-partitions.sh --bootstrap-server %s --reassignment-json-file %s --command-config %s --execute", bootstrapServer, reassignJsonPath, CLIENTS_CONFIG_FILE_PATH);
        LOGGER.debug("Execute reassign command => [{}]", reassignCommand);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", reassignCommand).out().trim();
        LOGGER.debug("Execute resulted in => [{}]", output);

        String verifyCommand = String.format("./bin/kafka-reassign-partitions.sh --bootstrap-server %s --reassignment-json-file %s --command-config %s --verify", bootstrapServer, reassignJsonPath, CLIENTS_CONFIG_FILE_PATH);
        LOGGER.debug("Verify reassign command => [{}]", verifyCommand);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", verifyCommand).out().trim();
        LOGGER.debug("Verify resulted in => [{}]", output);

        String removeFilesCommand = String.format("rm -f %s %s", reassignJsonPath, CLIENTS_CONFIG_FILE_PATH);
        LOGGER.debug("Remove created files command => [{}]", removeFilesCommand);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", removeFilesCommand).out().trim();
        LOGGER.debug("Removal resulted in => [{}]", output);
    }

    /**
     * Retrieves the committed offset for a given Kafka consumer group and topic from within a specified pod.
     * <p>
     * This method performs the following steps:
     * <ul>
     *     <li>Constructs and writes client properties to a temporary file in the pod.</li>
     *     <li>Executes the {@code kafka-consumer-groups.sh --describe} command inside the pod to get the offset.</li>
     *     <li>Parses and returns the offset for the specified topic.</li>
     * </ul>
     *
     * @param namespaceName      the namespace where the pod resides
     * @param kafkaName          the name of the Kafka cluster
     * @param podName            the name of the pod where the command should be executed
     * @param consumerGroupName  the name of the consumer group to query
     * @param topicName          the topic whose offset should be retrieved
     * @param clientsConfig   the content of the client.properties file (e.g., SASL config)
     * @return the committed offset as a {@link String}, or an empty string if not found
     */
    public static String getConsumerGroupOffset(String namespaceName, String kafkaName, String podName, String consumerGroupName, String topicName, String clientsConfig) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);

        LOGGER.info("Retrieve consumer group {} offset", consumerGroupName);

        insertClientProperties(namespaceName, podName, clientsConfig);

        String getOffsetCommand = String.format("./bin/kafka-consumer-groups.sh --bootstrap-server=%s --command-config=%s --group=%s --describe 2>/dev/null \\\n" +
            "  | awk -v topic=%s '$2 == topic { print $4 }' \\\n" +
            "  | grep -E '^[0-9]+$'", bootstrapServer, CLIENTS_CONFIG_FILE_PATH, consumerGroupName, topicName);
        LOGGER.debug("Run get consumergroup offset command");
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", getOffsetCommand).out().trim();
        LOGGER.debug("Get offset command resulted in => [{}]", output);

        return output;
    }

    /**
     * Verifies that a Kafka consumer group has no longer a committed offsets for a specific topic.
     *
     * <p>The method first ensures that the target topic exists in the Kafka cluster.
     * It then describes the specified consumer group and checks whether any numeric
     * offsets are still associated with the given topic.</p>
     *
     * <p>The verification is performed by executing the {@code kafka-consumer-groups.sh}
     * script inside the Kafka pod using inserted client configuration.</p>
     *
     * @param namespaceName     Kubernetes namespace where Kafka is deployed
     * @param kafkaName         Name of the Kafka cluster
     * @param podName           Name of the Kafka pod used to execute the command
     * @param consumerGroupName Name of the consumer group to verify
     * @param topicName         Topic for which offsets should be deleted
     * @param clientsConfig     Kafka client configuration used for authentication
     *
     * @return {@code true} if no offsets are found for the given topic in the consumer group,
     *         {@code false} otherwise
     */
    public static boolean verifyConsumerGroupHasDeletedOffsets(String namespaceName, String kafkaName, String podName, String consumerGroupName, String topicName, String clientsConfig) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);

        // Verify that the topic is present
        assertTrue(listKafkaTopicsByPrefix(namespaceName, kafkaName, podName, clientsConfig, topicName).contains(topicName));

        LOGGER.info("Retrieve consumer group {} offset", consumerGroupName);
        insertClientProperties(namespaceName, podName, clientsConfig);

        // Verify that the topic offset is not listed in describe
        String getOffsetCommand = String.format("./bin/kafka-consumer-groups.sh --bootstrap-server=%s --command-config=%s --group=%s --describe 2>/dev/null \\\n" +
            "  | awk -v topic=%s '$2 == topic { print $4 }' \\\n" +
            "  | grep -E '^[0-9]+$'", bootstrapServer, CLIENTS_CONFIG_FILE_PATH, consumerGroupName, topicName);
        LOGGER.debug("Run get consumergroup offset command");

        // Set throw to false as it returns an empty output
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(false, podName, Constants.BASH_CMD, "-c", getOffsetCommand).out().trim();
        LOGGER.debug("Get offset command resulted in => [{}]", output);
        return output.isEmpty();
    }

    /**
     * Sets the offset for a given Kafka consumer group and topic to a specific value.
     *
     * <p>This method executes the {@code kafka-consumer-groups.sh} CLI command inside the specified Kafka broker pod
     * to reset offsets to a given position using the {@code --to-offset} option.</p>
     *
     * <p>Before executing, it configures the Kafka client properties file in the pod to allow secure communication
     * based on the provided client configuration. The method logs both the intended operation and the CLI output for
     * debugging and traceability.</p>
     *
     * <p>This is useful for preparing test scenarios or restoring a specific consumer group state when verifying
     * offset reset behavior.</p>
     *
     * @param namespaceName       the Kubernetes namespace containing the Kafka cluster
     * @param kafkaName           the name of the Kafka cluster
     * @param podName             the name of the broker pod where the command will be executed
     * @param consumerGroupName   the name of the consumer group whose offset should be set
     * @param topicName           the topic for which the offset will be set
     * @param offset              the offset value to set (as a string)
     * @param clientsConfig       the Kafka client configuration content to be inserted before running the command
     */
    public static void setConsumerGroupOffset(String namespaceName, String kafkaName, String podName, String consumerGroupName, String topicName, String offset, String clientsConfig) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);

        LOGGER.info("Set consumer group {} offset to {}", consumerGroupName, offset);

        insertClientProperties(namespaceName, podName, clientsConfig);

        String setOffsetCmd = String.format("./bin/kafka-consumer-groups.sh --bootstrap-server=%s --command-config=%s " +
            "--group=%s --topic=%s --reset-offsets --to-offset=%s --execute", bootstrapServer, CLIENTS_CONFIG_FILE_PATH, consumerGroupName, topicName, offset);

        LOGGER.debug("Run consumer groups command to set the offset");
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", setOffsetCmd).out().trim();
        LOGGER.debug("Set offset command resulted in => [{}]", output);
    }

    /**
     * Lists Kafka topics from a given cluster that start with the specified prefix.
     *
     * <p>The method executes the {@code kafka-topics.sh --list} command inside the specified Kafka pod
     * using the provided client configuration. The output is filtered by the given topic prefix to return
     * only matching topics.</p>
     *
     * <p>This is useful for verifying topic creation during tests or for interacting with a subset of topics
     * that share a common naming pattern.</p>
     *
     * @param namespaceName the Kubernetes namespace where the Kafka cluster is running
     * @param kafkaName the name of the Kafka cluster
     * @param podName the name of the Kafka broker pod in which to run the command
     * @param clientsConfig the Kafka client configuration file contents for authentication/authorization
     * @param topicPrefix the prefix to filter topics by
     * @return a list of topic names that begin with the given prefix
     */
    public static List<String> listKafkaTopicsByPrefix(String namespaceName, String kafkaName, String podName, String clientsConfig, String topicPrefix) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);
        insertClientProperties(namespaceName, podName, clientsConfig);

        String listTopicsCmd = String.format("./bin/kafka-topics.sh --bootstrap-server=%s --command-config=%s --list 2>/dev/null | grep '^%s'", bootstrapServer, CLIENTS_CONFIG_FILE_PATH, topicPrefix);

        LOGGER.debug("Execute list topics command");
        List<String> output = Arrays.stream(KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", listTopicsCmd)
            .out().trim().split("\\r?\\n")).toList();

        LOGGER.debug("Listing topics resulted in => {}", output.toString());
        return output;
    }

    /**
     * Inserts Kafka client configuration into a file inside the specified pod.
     *
     * <p>The configuration string is written to the predefined client configuration file path
     * within the given pod, enabling subsequent Kafka CLI commands to authenticate and execute
     * with the provided settings.</p>
     *
     * <p>This is typically required before running Kafka commands such as listing topics or
     * managing consumer groups, as those commands rely on the client configuration file for
     * connection and authentication.</p>
     *
     * @param namespaceName the Kubernetes namespace where the Kafka pod is running
     * @param podName the name of the pod in which the configuration file should be created
     * @param clientsConfig the Kafka client configuration content to write into the file
     */
    public static void insertClientProperties(String namespaceName, String podName, String clientsConfig) {
        LOGGER.info("Insert client config");
        String insertConfigCommand = String.format("echo '%s' > %s", clientsConfig, CLIENTS_CONFIG_FILE_PATH);
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", insertConfigCommand).out().trim();
        LOGGER.debug("Insert client config resulted in => [{}]", output);
    }

    /**
     * Waits until a specific Kafka Connect connector is registered and visible in the
     * Kafka Connect REST API of the given Kafka Connect cluster.
     *
     * <p>This method retrieves the pod for the specified Kafka Connect cluster and
     * repeatedly queries its REST API endpoint for the given connector. It waits until
     * the API response contains the expected connector name in JSON format or the timeout
     * is reached.
     *
     * <p>Logs are emitted at each polling step:
     * <ul>
     *     <li>{@code info} when the connector is detected in the API response</li>
     *     <li>{@code warn} when the output is empty or does not yet contain the expected string</li>
     * </ul>
     *
     * @param namespace the Kubernetes namespace where the Kafka Connect cluster is deployed
     * @param connectName the name of the Kafka Connect cluster
     * @param connectorName the name of the connector to wait for in the Connect API
     */
    public static void waitForConnectorInServiceApi(String namespace, String connectName, String connectorName) {
        String podName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespace, connectName).get(0).getMetadata().getName();
        String service = KafkaConnectResources.url(connectName, namespace, Constants.CONNECT_SERVICE_PORT);
        String expectedLog = "\"connector\":\"" + connectorName + "\"";

        Wait.until(String.format("Pod %s/%s to contain log [%s]", namespace, podName, expectedLog),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_SHORT,
            () -> {
                String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).execInPod(podName, "/bin/bash", "-c", "curl " + service + "/connectors/" + connectorName).out();

                if (output == null || output.isEmpty()) {
                    LOGGER.warn("Connector output for connector {}/{} was empty or null", namespace, connectorName);
                    return false;
                }
                if (output.contains(expectedLog)) {
                    LOGGER.info("Connector output contains expected string {}/{}. Output: [{}]", namespace, expectedLog, output);
                    return true;
                }
                LOGGER.warn("Connector output does not yet contain expected string {}/{}. Output: [{}]", namespace, expectedLog, output);
                return false;
            });
    }
}
