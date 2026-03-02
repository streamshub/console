package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.utils.Utils;

import static com.github.streamshub.systemtests.utils.Utils.hashStub;

public class KafkaNamingUtils {
    private KafkaNamingUtils() {}

    // -------------
    // Kafka names
    // -------------
    public static String kafkaClusterName(String namespaceName) {
        return Constants.KAFKA_CLUSTER_PREFIX + "-" + hashStub(namespaceName);
    }

    public static String kafkaConnectName(String namespaceName) {
        return Constants.KAFKA_CONNECT_CLUSTER_PREFIX + "-" + hashStub(namespaceName);
    }

    public static String kafkaUserName(String kafkaName) {
        return Constants.KAFKA_USER_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String kafkaProducerName(String kafkaName) {
        return Constants.KAFKA_USER_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String kafkaConsumerName(String kafkaName) {
        return Constants.KAFKA_USER_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String brokerPoolName(String kafkaName) {
        return Constants.BROKER_ROLE_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String topicPrefixName(String kafkaName) {
        return Constants.KAFKA_TOPIC_PREFIX + "-" + Utils.hashStub(kafkaName);
    }

    public static String brokerPodNamePrefix(String kafkaName) {
        return kafkaName + "-" + Constants.BROKER_ROLE_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String controllerPoolName(String kafkaName) {
        return Constants.CONTROLLER_ROLE_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String kafkaMetricsConfigMapName(String kafkaName) {
        return kafkaName + "-metrics";
    }

    // Clients

    public static String producerName(String topicName) {
        return topicName + "-producer";
    }

    public static String consumerName(String topicName) {
        return topicName + "-consumer";
    }

    public static String consumerGroupName(String topicName) {
        return topicName + "-consumer-group";
    }
}
