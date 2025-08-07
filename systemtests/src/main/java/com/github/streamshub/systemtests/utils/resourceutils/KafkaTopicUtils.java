package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.IntStream;

public class KafkaTopicUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaTopicUtils.class);
    
    private KafkaTopicUtils() {}

    /**
     * Creates multiple Kafka topics with a specified prefix and returns the list of created {@link KafkaTopic} resources.
     *
     * <p>This method generates topic names using the provided {@code topicNamePrefix} followed by an index (e.g., {@code my-topic-0}, {@code my-topic-1}, etc.).</p>
     * <p>Each topic is configured with the specified number of {@code partitions}, {@code replicas}, and {@code minIsr} (minimum in-sync replicas).</p>
     * <p>Depending on the {@code waitForTopics} flag, the method either waits for the topics to be fully created or proceeds without waiting.</p>
     *
     * @param namespace the namespace in which the topics will be created
     * @param kafkaName the name of the Kafka cluster these topics belong to
     * @param topicNamePrefix the prefix used to generate topic names
     * @param numberToCreate the number of topics to create
     * @param waitForTopics whether to wait for the topics to become ready
     * @param partitions the number of partitions for each topic
     * @param replicas the replication factor for each topic
     * @param minIsr the minimum number of in-sync replicas required for writes
     * @return a list of {@link KafkaTopic} objects representing the created topics
     */
    public static List<KafkaTopic> setupTopicsAndReturn(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate, boolean waitForTopics, int partitions, int replicas, int minIsr) {
        LOGGER.info("Create {} topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);

        List<KafkaTopic> topics = IntStream.range(0, numberToCreate)
            .mapToObj(i -> defaultTopic(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr).build())
            .toList();

        if (waitForTopics) {
            KubeResourceManager.get().createResourceWithWait(topics.toArray(new KafkaTopic[0]));
        } else {
            KubeResourceManager.get().createResourceWithoutWait(topics.toArray(new KafkaTopic[0]));
        }
        return topics;
    }

    /**
     * Sets up a list of Kafka topics that will become under-replicated by intentionally scaling down brokers.
     *
     * <p>The method simulates an under-replicated scenario by first scaling up the broker pool to allow distribution of partition replicas across more brokers.</p>
     * <p>It creates the specified number of topics with given configurations, sends messages to each topic using a Kafka client, and then scales the broker pool down again, removing a broker that holds replicas.</p>
     * <p>To allow this behavior, it temporarily annotates the Kafka resource to bypass Strimzi's broker scaledown check.</p>
     * <p>This setup is useful for testing Kafka behavior and UI under under-replication conditions.</p>
     *
     * @param namespace the namespace where the Kafka cluster and topics are located
     * @param kafkaName the name of the Kafka cluster
     * @param kafkaUser the Kafka user used for authentication and producing/consuming messages
     * @param topicNamePrefix the prefix for generating topic names
     * @param numberToCreate the number of topics to create
     * @param messageCount the number of messages to produce and consume per topic
     * @param partitions the number of partitions per topic
     * @param replicas the replication factor for each topic
     * @param minIsr the minimum number of in-sync replicas required for write operations
     * @return a list of created {@link KafkaTopic} resources that are now under-replicated
     */
    public static List<KafkaTopic> setupUnderReplicatedTopicsAndReturn(String namespace, String kafkaName, String kafkaUser, String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) {
        LOGGER.info("Create {} underreplicated topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);
        /*
         * Under replicated Kafka Topic is a topic that has multiple partition replicas but 1 is on a Broker that gets deleted.
         * Normally Broker would not be deleted as there is a partition on it, but that can be bypassed with annotation.
         */

        // Scale Brokers Up
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaNodePoolBuilder(knp)
                .editSpec()
                    .withReplicas(knp.getSpec().getReplicas() + 1)
                .endSpec()
                .build());

        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() + 1, true);

        // Create new topics for under replication
        List<KafkaTopic> kafkaTopics = KafkaTopicUtils.setupTopicsAndReturn(namespace, kafkaName, topicNamePrefix, numberToCreate, true, partitions, replicas, minIsr);

        kafkaTopics.forEach(kt -> {
            KafkaClients clients = new KafkaClientsBuilder()
                .withNamespaceName(namespace)
                .withTopicName(kt.getMetadata().getName())
                .withMessageCount(messageCount)
                .withDelayMs(0)
                .withProducerName(KafkaNamingUtils.producerName(kt.getMetadata().getName()))
                .withConsumerName(KafkaNamingUtils.consumerName(kt.getMetadata().getName()))
                .withConsumerGroup(KafkaNamingUtils.consumerGroupName(kt.getMetadata().getName()))
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
                .withUsername(kafkaUser)
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(namespace, kafkaUser, SecurityProtocol.SASL_PLAINTEXT))
                .build();

            KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // Annotate Strimzi Kafka to allow broker scaledown without checking the brokers and topics
        // https://strimzi.io/blog/2024/01/03/prevent-broker-scale-down-if-containing-paritition-replicas/
        KafkaUtils.addAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, "true", true);

        // Scale down brokers
        knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaNodePoolBuilder(knp)
                .editSpec()
                    .withReplicas(knp.getSpec().getReplicas() - 1)
                .endSpec()
                .build());

        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() - 1, true);

        KafkaUtils.removeAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, true);
        return kafkaTopics;
    }

    /**
     * Sets up Kafka topics that will become unavailable by intentionally assigning their only partition to a broker that is later removed.
     *
     * <p>This simulates an unavailable topic scenario where each topic has a single partition and its only replica is moved to a broker that will be deleted.</p>
     * <p>The method first scales up the broker pool to introduce an additional broker, then creates the topics and reassigns their partitions to this new broker.</p>
     * <p>Kafka clients produce and consume messages to ensure topics are operational before the broker is removed.</p>
     * <p>After partition reassignment, the Kafka resource is annotated to allow Strimzi to bypass the partition check, and the broker hosting the partition is scaled down.</p>
     * <p>This leads to topics becoming unavailable, which is useful for testing failure handling in Kafka or UI indicators.</p>
     *
     * @param namespace the namespace where the Kafka cluster and topics exist
     * @param kafkaName the name of the Kafka cluster
     * @param kafkaUser the Kafka user used for SCRAM-SHA authentication and client operations
     * @param topicNamePrefix the prefix for naming the created topics
     * @param numberToCreate the number of unavailable topics to create
     * @param messageCount the number of messages to produce and consume per topic
     * @param partitions the number of partitions per topic (should typically be 1 for this scenario)
     * @param replicas the replication factor per topic (should typically be 1 for unavailability)
     * @param minIsr the minimum number of in-sync replicas
     * @return a list of created {@link KafkaTopic} resources that are now unavailable due to broker deletion
     */
    public static List<KafkaTopic> setupUnavailableTopicsAndReturn(String namespace, String kafkaName, String kafkaUser, String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) {
        /*
         * Unavailable Kafka Topic is a topic that has its only existing partition reassigned to a Broker that gets deleted
         */

        // Scale up brokers
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaNodePoolBuilder(knp)
                .editSpec()
                    .withReplicas(knp.getSpec().getReplicas() + 1)
                .endSpec()
                .build());

        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() + 1, true);
        List<KafkaTopic> kafkaTopics = setupTopicsAndReturn(namespace, kafkaName, topicNamePrefix, numberToCreate, true, partitions, replicas, minIsr);

        // Reassign the topic partition to last created broker that will be deleted
        // https://strimzi.io/blog/2022/09/16/reassign-partitions/
        List<Integer> brokerIds = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName)).getStatus().getNodeIds();
        int lastBrokerId = brokerIds.stream().sorted().toList().get(brokerIds.size() - 1);

        kafkaTopics.forEach(kt -> {
            KafkaClients clients = new KafkaClientsBuilder()
                .withNamespaceName(namespace)
                .withTopicName(kt.getMetadata().getName())
                .withMessageCount(messageCount)
                .withDelayMs(0)
                .withProducerName(KafkaNamingUtils.producerName(kt.getMetadata().getName()))
                .withConsumerName(KafkaNamingUtils.consumerName(kt.getMetadata().getName()))
                .withConsumerGroup(KafkaNamingUtils.consumerGroupName(kt.getMetadata().getName()))
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
                .withUsername(kafkaUser)
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(namespace, kafkaUser, SecurityProtocol.SASL_PLAINTEXT))
                .build();

            reassignTopicPartitionToAnotherBroker(namespace, kafkaName, ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespace,
                KafkaNamingUtils.brokerPodNamePrefix(kafkaName)).get(0).getMetadata().getName(), kt.getMetadata().getName(), lastBrokerId, clients);

            // Produce + consume messages
            KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // Annotate Kafka to allow broker scale down
        // https://strimzi.io/blog/2024/01/03/prevent-broker-scale-down-if-containing-paritition-replicas/
        KafkaUtils.addAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, "true", true);

        // Scale down brokers
        knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaNodePoolBuilder(knp)
                .editSpec()
                    .withReplicas(knp.getSpec().getReplicas() - 1)
                .endSpec()
                .build());

        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() - 1, true);

        KafkaUtils.removeAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, true);
        return kafkaTopics;
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
        String clientConfigPath = "/tmp/client.properties";

        String insertPropertiesCommand = String.format("echo '%s' > %s", clients.getAdditionalConfig(), clientConfigPath);
        LOGGER.debug("Insert client config");
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", insertPropertiesCommand).out().trim();
        LOGGER.debug("Inserting resulted in => [{}]", output);

        String reassignJson = String.format("echo '{\"version\":1,\"partitions\":[{\"topic\":\"%s\",\"partition\":0,\"replicas\":[%d],\"log_dirs\":[\"any\"]}]}' >> %s", topicName, newBrokerId, reassignJsonPath);
        LOGGER.debug("Insert reassign json => [{}]", reassignJson);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", reassignJson).out().trim();
        LOGGER.debug("Inserting resulted in => [{}]", output);

        String reassignCommand = String.format("./bin/kafka-reassign-partitions.sh --bootstrap-server %s --reassignment-json-file %s --command-config %s --execute", bootstrapServer, reassignJsonPath, clientConfigPath);
        LOGGER.debug("Execute reassign command => [{}]", reassignCommand);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", reassignCommand).out().trim();
        LOGGER.debug("Execute resulted in => [{}]", output);

        String verifyCommand = String.format("./bin/kafka-reassign-partitions.sh --bootstrap-server %s --reassignment-json-file %s --command-config %s --verify", bootstrapServer, reassignJsonPath, clientConfigPath);
        LOGGER.debug("Verify reassign command => [{}]", verifyCommand);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", verifyCommand).out().trim();
        LOGGER.debug("Verify resulted in => [{}]", output);

        String removeFilesCommand = String.format("rm -f %s %s", reassignJsonPath, clientConfigPath);
        LOGGER.debug("Remove created files command => [{}]", removeFilesCommand);
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", removeFilesCommand).out().trim();
        LOGGER.debug("Removal resulted in => [{}]", output);
    }

    public static KafkaTopicBuilder defaultTopic(String topicNamespace, String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        return new KafkaTopicBuilder()
            .withNewMetadata()
                .withName(topicName)
                .withNamespace(topicNamespace)
                .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName)
            .endMetadata()
            .editSpec()
                .withPartitions(partitions)
                .withReplicas(replicas)
                .addToConfig("min.insync.replicas", minIsr)
            .endSpec();
    }

    /**
     * Creates a list of Kafka topics by producing and consuming messages directly to them without creating corresponding
     * {@code KafkaTopic} custom resources in the Kubernetes cluster (i.e., the topics remain unmanaged by Strimzi).
     *
     * <p>This method is useful for testing scenarios where topics are created dynamically by clients
     * (outside the control of Strimzi Topic Operator), for example to test how the system handles unmanaged topics.</p>
     *
     * <p>The following actions are performed for each topic:</p>
     * <ol>
     *   <li>Builds a {@code KafkaTopic} definition (not applied to the cluster).</li>
     *   <li>Creates Kafka clients (producer and consumer) with SCRAM-SHA authentication.</li>
     *   <li>Produces and consumes messages to ensure the topic exists on the Kafka broker.</li>
     * </ol>
     *
     * @param namespace the Kubernetes namespace where the Kafka cluster is running
     * @param kafkaName the name of the Kafka cluster
     * @param kafkaUser the name of the KafkaUser resource for SCRAM-SHA authentication
     * @param topicNamePrefix the prefix to use for the topic names
     * @param numberToCreate the number of topics to create
     * @param messageCount the number of messages to produce and consume for each topic
     * @param partitions the number of partitions per topic
     * @param replicas the number of replicas per topic
     * @param minIsr the minimum number of in-sync replicas for the topic
     * @return a list of topic names that were created and used, but not managed by the Topic Operator
     */
    public static List<String> setupUnmanagedTopicsAndReturnNames(String namespace, String kafkaName, String kafkaUser, String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) {
        List<KafkaTopic> topics = IntStream.range(0, numberToCreate)
            .mapToObj(i -> defaultTopic(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr).build())
            .toList();

        topics.forEach(kt -> {
            KafkaClients clients =  new KafkaClientsBuilder()
                .withNamespaceName(namespace)
                .withTopicName(kt.getMetadata().getName())
                .withMessageCount(messageCount)
                .withDelayMs(0)
                .withProducerName(KafkaNamingUtils.producerName(kt.getMetadata().getName()))
                .withConsumerName(KafkaNamingUtils.consumerName(kt.getMetadata().getName()))
                .withConsumerGroup(KafkaNamingUtils.consumerGroupName(kt.getMetadata().getName()))
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
                .withUsername(kafkaUser)
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(namespace, kafkaUser, SecurityProtocol.SASL_PLAINTEXT))
                .build();
            KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // returning names, since no CR is created on cluster side
        return topics.stream().map(kt -> kt.getMetadata().getName()).toList();
    }

    public static String getConsumerOffsetTimestampFromOffset(String namespaceName, String kafkaName, String podName, String topicName, String clientsConfig, String offset, int partition, int maxMessages) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);
        String clientConfigPath = "/tmp/client.properties";

        String insertPropertiesCommand = String.format("echo '%s' > %s", clientsConfig, clientConfigPath);
        LOGGER.debug("Insert client config");
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", insertPropertiesCommand).out().trim();
        LOGGER.debug("Inserting resulted in => [{}]", output);

        String getOffsetCommand = String.format("./bin/kafka-console-consumer.sh --bootstrap-server=%s --consumer.config=%s --topic=%s --offset=%s --partition=%d --max-messages=%d --property=print.timestamp=true 2>/dev/null" +
                                                " | awk -F'[:\\t]' '/CreateTime:/ {print $2}'", bootstrapServer, clientConfigPath, topicName, offset, partition, maxMessages);
        LOGGER.debug("Execute get offset command");
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", getOffsetCommand).out().trim();
        LOGGER.debug("Execution resulted in => [{}]", output);

        return output;
    }
}
