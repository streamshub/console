package com.github.streamshub.systemtests.templates;

import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.ExampleFileConstants;
import com.github.streamshub.systemtests.utils.YamlUtils;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;

public class KafkaTopicTemplates {

    private KafkaTopicTemplates() {}

    public static KafkaTopicBuilder topic(String topicNamespace, String clusterName, String topicName) {
        return defaultTopic(topicNamespace, clusterName, topicName, 1, 1, 1);
    }

    public static KafkaTopicBuilder topic(String topicNamespace, String clusterName, String topicName, int partitions) {
        return defaultTopic(topicNamespace, clusterName, topicName, partitions, 1, 1);
    }

    public static KafkaTopicBuilder topic(String topicNamespace, String clusterName, String topicName, int partitions, int replicas) {
        return defaultTopic(topicNamespace, clusterName, topicName, partitions, replicas, replicas);
    }

    public static KafkaTopicBuilder topic(String topicNamespace, String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        return defaultTopic(topicNamespace, clusterName, topicName, partitions, replicas, minIsr);
    }

    public static KafkaTopicBuilder defaultTopic(String topicNamespace, String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        KafkaTopic kafkaTopic = getKafkaTopicFromYaml(ExampleFileConstants.EXAMPLES_KAFKA_TOPIC_YAML);
        return new KafkaTopicBuilder(kafkaTopic)
            .withNewMetadata()
                .withName(topicName)
                .withNamespace(topicNamespace)
                .addToLabels(Labels.STRIMZI_CLUSTER, clusterName)
            .endMetadata()
            .editSpec()
                .withPartitions(partitions)
                .withReplicas(replicas)
                .addToConfig("min.insync.replicas", minIsr)
            .endSpec();
    }

    private static KafkaTopic getKafkaTopicFromYaml(String yamlPath) {
        return YamlUtils.configFromYaml(yamlPath, KafkaTopic.class);
    }
}
