package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.constants.Constants;

import static com.github.streamshub.systemtests.utils.Utils.hashStub;

public class KafkaUtils {

    // -------------
    // Kafka names
    // -------------
    public static String kafkaClusterName(String namespaceName) {
        return Constants.KAFKA_CLUSTER_PREFIX + "-" + hashStub(namespaceName);
    }

    public static String kafkaUserName(String kafkaName) {
        return Constants.KAFKA_USER_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String brokerPoolName(String kafkaName) {
        return Constants.BROKER_ROLE_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String brokerPodName(String kafkaName) {
        return Constants.BROKER_ROLE_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String controllerPoolName(String kafkaName) {
        return Constants.CONTROLLER_ROLE_PREFIX + "-" + hashStub(kafkaName);
    }

    public static String kafkaMetricsConfigMapName(String kafkaName) {
        return kafkaName + "-metrics";
    }
}
