package com.github.streamshub.systemtests.constants;

import java.io.File;

import static io.skodjob.testframe.TestFrameEnv.USER_PATH;

public class ExampleFiles {
    private ExampleFiles() {}

    // ---------------
    // Install resources
    // --------------
    public static final String EXAMPLES_PATH = USER_PATH + "/../examples";
    // -----
    // Kafka
    // -----
    public static final String EXAMPLE_KAFKA_PATH = EXAMPLES_PATH + "/kafka/";
    public static final File EXAMPLES_KAFKA_METRICS_CONFIG_MAP = new File(EXAMPLE_KAFKA_PATH + "010-ConfigMap-console-kafka-metrics.yaml");
}
