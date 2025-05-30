package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.IntStream;

public class KafkaTopicUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaTopicUtils.class);
    
    private KafkaTopicUtils() {}
    
    public static List<KafkaTopic> createTopicsAndReturn(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate, boolean waitForTopic, int partitions, int replicas, int minIsr) {
        LOGGER.info("Create {} topics for cluster {} with topic name prefix {}", numberToCreate, kafkaName, topicNamePrefix);

        List<KafkaTopic> topics = IntStream.range(0, numberToCreate)
            .mapToObj(i -> defaultTopic(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr).build())
            .toList();

        topics.forEach(KubeResourceManager.get()::createResourceWithoutWait);

        return topics;
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
}
