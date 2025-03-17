package com.github.streamshub.systemtests.constants;

public interface Constants {
    // --------------------------------
    // ------------ General -----------
    // --------------------------------
    /**
     * Namespaces
     */
    String CO_NAMESPACE = "co-namespace";
    String OPENSHIFT_MARKETPLACE_NAMESPACE = "openshift-marketplace";

    // --------------------------------
    // ----------- Strimzi ------------
    // --------------------------------
    String STRIMZI_NAMESPACE_KEY = "STRIMZI_NAMESPACE";
    String STRIMZI_WATCH_ALL_NAMESPACES = "*";
    String STRIMZI_DOMAIN = "strimzi.io/";

    // --------------------------------
    // ------------ Console -----------
    // --------------------------------
    /**
     * CRDs
     */
    String CONSOLE_CRD_API_VERSION = "console.streamshub.github.com/v1alpha1";
    String CONSOLE_OPERATOR_GROUP_NAME = "streamshub-operators";
    String CONSOLE_INSTANCE = "console-instance";
    /**
     * Deployments
     */
    String CONSOLE_DEPLOYMENT = "console-deployment";
    String PROMETHEUS_DEPLOYMENT = "prometheus-deployment";

    // --------------------------------
    // ------------- Kafka ------------
    // --------------------------------
    /**
     *  Kafka NodePool
     */
    String PRODUCER = "producer";
    String CONSUMER = "consumer";
    String USER = "user";
    String KAFKA_CLUSTER_PREFIX = "kc";
    String KAFKA_TOPIC_PREFIX = "kt";
    String BROKER_ROLE_PREFIX = "brk";
    String CONTROLLER_ROLE_PREFIX = "ctrl";
    String MIXED_ROLE_PREFIX = "mx";

    // --------------------------------
    // --------- TEST VALUES ----------
    // --------------------------------
    /**
     * Messages
     */
    int MESSAGE_COUNT = 1_000;
    int REGULAR_KAFKA_REPLICAS = 3;
    /**
     * Consumers
     */
    String CONSUMER_STATE_STABLE = "Stable";
    String CONSUMER_STATE_EMPTY = "Empty";
    String CONSUMER_OFFSETS_TOPIC_NAME = "__consumer_offsets";
}
