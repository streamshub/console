package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.clients.KafkaClients;
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
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.IntStream;

public class KafkaTopicUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaTopicUtils.class);
    
    private KafkaTopicUtils() {}
    
    public static List<KafkaTopic> createTopicsAndReturn(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate, boolean waitForTopics, int partitions, int replicas, int minIsr) {
        LOGGER.info("Create {} topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);

        List<KafkaTopic> topics = IntStream.range(0, numberToCreate)
            .mapToObj(i -> defaultTopic(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr).build())
            .toList();

        if (waitForTopics) {
            topics.forEach(KubeResourceManager.get()::createResourceWithWait);
        } else {
            topics.forEach(KubeResourceManager.get()::createResourceWithoutWait);
        }
        return topics;
    }


    public static List<KafkaTopic> createUnderReplicatedTopicsAndReturn(String namespace, String kafkaName, String kafkaUser, String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) {
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

        WaitUtils.waitForPodsReady(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() + 1, true);

        // Create new topics for under replication
        List<KafkaTopic> kafkaTopics = KafkaTopicUtils.createTopicsAndReturn(namespace, kafkaName, topicNamePrefix, numberToCreate, true, partitions, replicas, minIsr);

        kafkaTopics.forEach(kt -> {
            KafkaClients clients = KafkaClientsUtils.scramShaPlainTextClientBuilder(namespace, kafkaName, kafkaUser, kt.getMetadata().getName(), messageCount).build();
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

        WaitUtils.waitForPodsReady(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() - 1, true);

        KafkaUtils.removeAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, true);
        return kafkaTopics;
    }

    public static List<KafkaTopic> createUnavailableTopicsAndReturn(String namespace, String kafkaName, String kafkaUser, String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) {
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

        WaitUtils.waitForPodsReady(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() + 1, true);
        List<KafkaTopic> kafkaTopics = createTopicsAndReturn(namespace, kafkaName, topicNamePrefix, numberToCreate, true, partitions, replicas, minIsr);

        // Reassign the topic partition to last created broker that will be deleted
        // https://strimzi.io/blog/2022/09/16/reassign-partitions/
        List<Integer> brokerIds = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName)).getStatus().getNodeIds();
        int lastBrokerId = brokerIds.stream().sorted().toList().get(brokerIds.size() - 1);

        kafkaTopics.forEach(kt -> {
            KafkaClients clients = KafkaClientsUtils.scramShaPlainTextClientBuilder(namespace, kafkaName, kafkaUser, kt.getMetadata().getName(), messageCount).build();

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

        WaitUtils.waitForPodsReady(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), knp.getSpec().getReplicas() - 1, true);

        KafkaUtils.removeAnnotation(namespace, kafkaName, ResourceAnnotations.ANNO_STRIMZI_IO_SKIP_BROKER_SCALEDOWN_CHECK, true);
        return kafkaTopics;
    }

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

    public static List<String> createUnmanagedTopicsAndReturn(String namespace, String kafkaName, String kafkaUser, String topicNamePrefix, int numberToCreate, int messageCount, int partitions, int replicas, int minIsr) {
        List<KafkaTopic> topics = IntStream.range(0, numberToCreate)
            .mapToObj(i -> defaultTopic(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr).build())
            .toList();

        topics.forEach(kt -> {
            KafkaClients clients = KafkaClientsUtils.scramShaPlainTextClientBuilder(namespace, kafkaName, kafkaUser, kt.getMetadata().getName(), messageCount).build();
            KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        });

        // returning names, since no CR is created on cluster side
        return topics.stream().map(kt -> kt.getMetadata().getName()).toList();
    }
}
