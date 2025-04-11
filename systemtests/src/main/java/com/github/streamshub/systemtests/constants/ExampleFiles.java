package com.github.streamshub.systemtests.constants;

import java.io.File;

import static io.skodjob.testframe.TestFrameEnv.USER_PATH;

public class ExampleFiles {
    private ExampleFiles() {}

    // ---------------
    // Install resources
    // --------------
    public static final String OPERATOR_INSTALL_PATH = USER_PATH + "/../install/operator";
    public static final String EXAMPLES_PATH = USER_PATH + "/../examples";
    public static final String CONSOLE_EXAMPLES_PATH = EXAMPLES_PATH + "/console/";
    // -----
    // OLM Console operator
    // -----
    public static final String OLM_RESOURCES_PATH = OPERATOR_INSTALL_PATH + "/olm/";
    public static final File CONSOLE_OPERATOR_GROUP = new File(OLM_RESOURCES_PATH + "000-OperatorGroup-console-operator.yaml");
    public static final File CONSOLE_OPERATOR_SUBSCRIPTION = new File(OLM_RESOURCES_PATH + "020-Subscription-console-operator.yaml");
    // -----
    // Console instance
    // -----
    public static final File EXAMPLE_CONSOLE_INSTANCE = new File(CONSOLE_EXAMPLES_PATH + "010-Console-example.yaml");
    // -----
    // Kafka
    // -----
    public static final String EXAMPLE_KAFKA_PATH = EXAMPLES_PATH + "/kafka/";
    public static final File EXAMPLES_KAFKA_TOPIC_YAML = new File(EXAMPLE_KAFKA_PATH + "050-KafkaTopic-console-topic.yaml");
    public static final File EXAMPLES_KAFKA_METRICS_CONFIG_MAP = new File(EXAMPLE_KAFKA_PATH + "010-ConfigMap-console-kafka-metrics.yaml");
    public static final File EXAMPLES_KAFKA_USER = new File(EXAMPLE_KAFKA_PATH + "040-KafkaUser-console-kafka-user1.yaml");
    public static final File EXAMPLES_KAFKA = new File(EXAMPLE_KAFKA_PATH + "030-Kafka-console-kafka.yaml");
    public static final File EXAMPLES_KAFKA_NODEPOOLS_BROKER = new File(EXAMPLE_KAFKA_PATH + "020-KafkaNodePool-broker-console-nodepool.yaml");
    public static final File EXAMPLES_KAFKA_NODEPOOLS_CONTROLLER = new File(EXAMPLE_KAFKA_PATH + "021-KafkaNodePool-controller-console-nodepool.yaml");
}
