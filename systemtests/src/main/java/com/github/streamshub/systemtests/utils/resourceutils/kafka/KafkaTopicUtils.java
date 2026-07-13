package com.github.streamshub.systemtests.utils.resourceutils.kafka;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
     * @param namespace       the namespace in which the topics will be created
     * @param kafkaName       the name of the Kafka cluster these topics belong to
     * @param topicNamePrefix the prefix used to generate topic names
     * @param numberToCreate  the number of topics to create
     * @param partitions      the number of partitions for each topic
     * @param replicas        the replication factor for each topic
     * @param minIsr          the minimum number of in-sync replicas required for writes
     * @return a list of {@link KafkaTopic} objects representing the created topics
     */
    public static List<KafkaTopic> setupTopicsIfNeededAndReturn(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate, int partitions, int replicas, int minIsr) {
        return setupTopicsIfNeededAndReturn(namespace, kafkaName, true, topicNamePrefix, numberToCreate, partitions, replicas, minIsr);
    }

    public static List<KafkaTopic> setupTopicsIfNeededAndReturn(String namespace, String kafkaName, boolean clearTopicsBefore, String topicNamePrefix, int numberToCreate, int partitions, int replicas, int minIsr) {
        LOGGER.info("Create {} topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);

        List<KafkaTopic> topics = IntStream.range(0, numberToCreate)
            .mapToObj(i -> defaultTopic(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr).build())
            .toList();

        if (clearTopicsBefore) {
            List<KafkaTopic> existingTopics = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, namespace, topicNamePrefix);
            LOGGER.info("Deleting {} existing topic(s) with prefix {} before recreation", existingTopics.size(), topicNamePrefix);
            KubeResourceManager.get().deleteResourceWithWait(existingTopics.toArray(new KafkaTopic[0]));
        }

        if (ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, namespace, topicNamePrefix).isEmpty()) {
            LOGGER.debug("Creating {} KafkaTopic resource(s) with prefix {} in namespace {}", topics.size(), topicNamePrefix, namespace);
            KubeResourceManager.get().createResourceAsyncWait(topics.toArray(new KafkaTopic[0]));
            return topics;
        }

        LOGGER.info("Topics with prefix {} are already present, skipping creation", topicNamePrefix);
        return ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, namespace, topicNamePrefix);
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
        int scaledBrokersCount = knp.getSpec().getReplicas() + 1;

        KafkaUtils.scaleBrokerReplicasWithWait(namespace, kafkaName, scaledBrokersCount);

        // Create new topics for under replication
        List<KafkaTopic> kafkaTopics = KafkaTopicUtils.setupTopicsIfNeededAndReturn(namespace, kafkaName, topicNamePrefix, numberToCreate, partitions, replicas, minIsr);

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

            KubeResourceManager.get().createResourceAsyncWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // Annotate Strimzi Kafka to allow broker scaledown without checking the brokers and topics
        // https://strimzi.io/blog/2024/01/03/prevent-broker-scale-down-if-containing-paritition-replicas/
        KafkaUtils.addAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, "true", true);

        // Scale down brokers
        knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        scaledBrokersCount = knp.getSpec().getReplicas() - 1;

        KafkaUtils.scaleBrokerReplicasWithWait(namespace, kafkaName, scaledBrokersCount);

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
        LOGGER.info("Create {} unavailable topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);

        // Scale up brokers
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        int scaledBrokersCount = knp.getSpec().getReplicas() + 1;

        KafkaUtils.scaleBrokerReplicasWithWait(namespace, kafkaName, scaledBrokersCount);

        List<KafkaTopic> kafkaTopics = setupTopicsIfNeededAndReturn(namespace, kafkaName, topicNamePrefix, numberToCreate, partitions, replicas, minIsr);

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

            LOGGER.debug("Reassigning topic {} onto broker {} that will subsequently be scaled down", kt.getMetadata().getName(), lastBrokerId);
            KafkaCmdUtils.reassignTopicPartitionToAnotherBroker(namespace, kafkaName, ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespace,
                KafkaNamingUtils.brokerPodNamePrefix(kafkaName)).getFirst().getMetadata().getName(), kt.getMetadata().getName(), lastBrokerId, clients);

            // Produce + consume messages
            KubeResourceManager.get().createResourceAsyncWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // Annotate Kafka to allow broker scale down
        // https://strimzi.io/blog/2024/01/03/prevent-broker-scale-down-if-containing-paritition-replicas/
        KafkaUtils.addAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, "true", true);

        // Scale down brokers
        knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        scaledBrokersCount = knp.getSpec().getReplicas() - 1;

        KafkaUtils.scaleBrokerReplicasWithWait(namespace, kafkaName, scaledBrokersCount);

        KafkaUtils.removeAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, true);
        return kafkaTopics;
    }

    public static KafkaTopicBuilder defaultTopic(String topicNamespace, String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        return new KafkaTopicBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
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
        LOGGER.info("Create {} unmanaged topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);
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
            KubeResourceManager.get().createResourceAsyncWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // returning names, since no CR is created on cluster side
        return topics.stream().map(kt -> kt.getMetadata().getName()).toList();
    }

    /**
     * Groups the parameters needed to set up one type of topic (unmanaged, under-replicated, or unavailable)
     * via {@link #setupUnmanagedUnderReplicatedAndUnavailableTopics}.
     *
     * @param topicNamePrefix the prefix used to generate topic names
     * @param numberToCreate  the number of topics to create
     * @param messageCount    the number of messages to produce and consume per topic
     * @param partitions      the number of partitions for each topic
     * @param replicas        the replication factor for each topic
     * @param minIsr          the minimum number of in-sync replicas required for writes
     */
    public record TopicTypeSpec(String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) { }

    /**
     * Combined, time-optimized setup for unmanaged, under-replicated, and unavailable topics.
     *
     * <p>Unlike calling {@link #setupUnmanagedTopicsAndReturnNames}, {@link #setupUnderReplicatedTopicsAndReturn},
     * and {@link #setupUnavailableTopicsAndReturn} separately, this method shares a single broker
     * scale-up/scale-down cycle between the under-replicated and unavailable scenarios, overlaps the
     * broker scale-up wait with unmanaged topic setup, and waits for all producer/consumer clients across
     * all three topic groups concurrently instead of one topic at a time.</p>
     *
     * @param namespace           the namespace where the Kafka cluster and topics are located
     * @param kafkaName           the name of the Kafka cluster
     * @param kafkaUser           the Kafka user used for authentication and producing/consuming messages
     * @param unmanagedSpec       spec for the unmanaged topics
     * @param underReplicatedSpec spec for the under-replicated topics
     * @param unavailableSpec     spec for the unavailable topics
     */
    public static void setupUnmanagedUnderReplicatedAndUnavailableTopics(String namespace, String kafkaName, String kafkaUser,
            TopicTypeSpec unmanagedSpec, TopicTypeSpec underReplicatedSpec, TopicTypeSpec unavailableSpec) {
        LOGGER.info("Create unmanaged, under-replicated, and unavailable topics for cluster {} with a shared broker scale cycle", kafkaName);

        // Scale brokers up once in the background - shared by the under-replicated and unavailable scenarios
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        int scaledUpBrokersCount = knp.getSpec().getReplicas() + 1;
        LOGGER.debug("Starting asynchronous broker scale-up of cluster {} to {} replicas", kafkaName, scaledUpBrokersCount);
        CompletableFuture<Void> brokerScaleUp = CompletableFuture.runAsync(() -> KafkaUtils.scaleBrokerReplicasWithWait(namespace, kafkaName, scaledUpBrokersCount));

        // Meanwhile, create unmanaged topics (no broker dependency) and kick off their clients
        List<KafkaTopic> unmanagedTopics = IntStream.range(0, unmanagedSpec.numberToCreate())
            .mapToObj(i -> defaultTopic(namespace, kafkaName, unmanagedSpec.topicNamePrefix() + "-" + i, unmanagedSpec.partitions(), unmanagedSpec.replicas(), unmanagedSpec.minIsr()).build())
            .toList();
        List<KafkaClients> allClients = new ArrayList<>(buildAndCreateClients(namespace, kafkaName, kafkaUser, unmanagedTopics, unmanagedSpec.messageCount()));

        brokerScaleUp.join();

        // Now that the extra broker exists, create the under-replicated and unavailable topics
        List<KafkaTopic> underReplicatedTopics = KafkaTopicUtils.setupTopicsIfNeededAndReturn(namespace, kafkaName, underReplicatedSpec.topicNamePrefix(),
            underReplicatedSpec.numberToCreate(), underReplicatedSpec.partitions(), underReplicatedSpec.replicas(), underReplicatedSpec.minIsr());
        List<KafkaTopic> unavailableTopics = KafkaTopicUtils.setupTopicsIfNeededAndReturn(namespace, kafkaName, unavailableSpec.topicNamePrefix(),
            unavailableSpec.numberToCreate(), unavailableSpec.partitions(), unavailableSpec.replicas(), unavailableSpec.minIsr());

        allClients.addAll(buildAndCreateClients(namespace, kafkaName, kafkaUser, underReplicatedTopics, underReplicatedSpec.messageCount()));

        // Reassign each unavailable topic's partition onto the new broker
        // https://strimzi.io/blog/2022/09/16/reassign-partitions/
        List<Integer> brokerIds = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName)).getStatus().getNodeIds();
        int lastBrokerId = brokerIds.stream().sorted().toList().get(brokerIds.size() - 1);
        String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespace, KafkaNamingUtils.brokerPodNamePrefix(kafkaName)).getFirst().getMetadata().getName();

        List<KafkaClients> unavailableClients = unavailableTopics.stream()
            .map(kt -> buildClients(namespace, kafkaName, kafkaUser, kt.getMetadata().getName(), unavailableSpec.messageCount()))
            .toList();
        unavailableClients.forEach(clients -> KafkaCmdUtils.reassignTopicPartitionToAnotherBroker(namespace, kafkaName, brokerPodName, clients.getTopicName(), lastBrokerId, clients));

        allClients.addAll(createClientsAsync(unavailableClients));

        LOGGER.debug("Waiting for {} client pair(s) across unmanaged, under-replicated, and unavailable topics to finish", allClients.size());
        // Wait for all producer/consumer clients across all three topic groups at once
        WaitUtils.waitForClientsSuccess(allClients);

        // Annotate Strimzi Kafka to allow broker scaledown without checking the brokers and topics
        // https://strimzi.io/blog/2024/01/03/prevent-broker-scale-down-if-containing-paritition-replicas/
        KafkaUtils.addAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, "true", true);

        // Scale down brokers once, covering both scenarios
        knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        int scaledDownBrokersCount = knp.getSpec().getReplicas() - 1;
        KafkaUtils.scaleBrokerReplicasWithWait(namespace, kafkaName, scaledDownBrokersCount);

        KafkaUtils.removeAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, true);
    }

    private static KafkaClients buildClients(String namespace, String kafkaName, String kafkaUser, String topicName, int messageCount) {
        return new KafkaClientsBuilder()
            .withNamespaceName(namespace)
            .withTopicName(topicName)
            .withMessageCount(messageCount)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
            .withUsername(kafkaUser)
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(namespace, kafkaUser, SecurityProtocol.SASL_PLAINTEXT))
            .build();
    }

    private static List<KafkaClients> createClientsAsync(List<KafkaClients> clientsList) {
        KubeResourceManager.get().createResourceAsyncWait(clientsList.stream()
            .flatMap(clients -> Stream.of(clients.producer(), clients.consumer()))
            .toArray(Job[]::new));
        return clientsList;
    }

    private static List<KafkaClients> buildAndCreateClients(String namespace, String kafkaName, String kafkaUser, List<KafkaTopic> topics, int messageCount) {
        List<KafkaClients> clientsList = topics.stream()
            .map(kt -> buildClients(namespace, kafkaName, kafkaUser, kt.getMetadata().getName(), messageCount))
            .toList();
        return createClientsAsync(clientsList);
    }
}
