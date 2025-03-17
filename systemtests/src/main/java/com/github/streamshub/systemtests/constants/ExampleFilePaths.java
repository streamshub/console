package com.github.streamshub.systemtests.constants;

import static io.skodjob.testframe.TestFrameEnv.USER_PATH;

public interface ExampleFilePaths {

    // ---------------
    // Install resources
    // --------------
    public static final String OPERATOR_INSTALL_PATH = USER_PATH + "/../install/operator";
    public static final String EXAMPLES_PATH = USER_PATH + "/../examples";
    // -----
    // OLM Console operator
    // -----
    public static final String OLM_RESOURCES_PATH = OPERATOR_INSTALL_PATH + "/olm/";
    public static final String CONSOLE_OPERATOR_SUBSCRIPTION_YAML = OLM_RESOURCES_PATH + "020-Subscription-console-operator.yaml";
    // -----
    // Kafka
    // -----
    public static final String EXAMPLE_KAFKA_PATH = EXAMPLES_PATH + "/kafka/";
    public static final String EXAMPLES_KAFKA_TOPIC_YAML = EXAMPLE_KAFKA_PATH + "050-KafkaTopic-console-topic.yaml";
    public static final String EXAMPLES_KAFKA_METRICS_CONFIG_MAP_YAML = EXAMPLE_KAFKA_PATH + "010-ConfigMap-console-kafka-metrics.yaml";
    public static final String EXAMPLES_KAFKA_USER = EXAMPLE_KAFKA_PATH + "040-KafkaUser-console-kafka-user1.yaml";
    public static final String EXAMPLES_KAFKA = EXAMPLE_KAFKA_PATH + "030-Kafka-console-kafka.yaml";
    public static final String EXAMPLES_KAFKA_NODEPOOLS_BROKER = EXAMPLE_KAFKA_PATH + "021-KafkaNodePool-broker-console-nodepool.yaml";
    public static final String EXAMPLES_KAFKA_NODEPOOLS_CONTROLLER = EXAMPLE_KAFKA_PATH + "020-KafkaNodePool-controller-console-nodepool.yaml";
}
