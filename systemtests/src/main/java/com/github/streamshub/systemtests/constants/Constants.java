package com.github.streamshub.systemtests.constants;

public class Constants {
    private Constants() {}

    // --------------------------------
    // ------------ General -----------
    // --------------------------------
    public static final int MAX_ACTION_RETRIES = 10;
    /**
     * Cluster
     */
    public static final String OPENSHIFT_CONSOLE = "openshift-console";
    public static final String OPENSHIFT_CONSOLE_ROUTE_NAME = "console";

    /**
     * Commands
     */
    public static final String BASH_CMD = "/bin/bash";

    /**
     * Namespaces
     */
    public static final String CO_NAMESPACE = "co-namespace";
    public static final String OPENSHIFT_MARKETPLACE_NAMESPACE = "openshift-marketplace";

    /**
     * Test values
     */
    public static final String USER_USERNAME = "Anonymous";
    public static final int SELECTOR_RETRIES = 10;
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String CHECKED_ATTRIBUTE = "checked";

    /**
     * Messages
     */
    public static final int MESSAGE_COUNT = 100;
    public static final int MESSAGE_COUNT_HIGH = 10_000;

    /**
     * CRDs
     */
    public static final String RBAC_AUTH_API_GROUP = "rbac.authorization.k8s.io";

    // --------------------------------
    // ----------- Strimzi ------------
    // --------------------------------
    public static final String STRIMZI_NAME = "strimzi";
    public static final String STRIMZI_CO_NAME = STRIMZI_NAME + "-cluster-operator";
    public static final String STRIMZI_NAMESPACE_KEY = "STRIMZI_NAMESPACE";
    public static final String STRIMZI_WATCH_ALL_NAMESPACES = "*";

    /**
     * Clients
     */
    public static final String PRODUCER = "producer";
    public static final String CONSUMER = "consumer";

    /**
     * Topics
     */
    public static final String KAFKA_TOPIC_PREFIX = "kt";
    public static final String CONSUMER_OFFSETS_TOPIC_NAME = "__consumer_offsets";

    /**
     *  Kafka
     */
    public static final String KAFKA_USER_PREFIX = "ku";
    public static final String KAFKA_CLUSTER_PREFIX = "kc";
    public static final String BROKER_ROLE_PREFIX = "brk";
    public static final String CONTROLLER_ROLE_PREFIX = "ctrl";
    public static final int REGULAR_BROKER_REPLICAS = 3;
    public static final int REGULAR_CONTROLLER_REPLICAS = 3;

    /**
     * Listeners
     */
    public static final String PLAIN_LISTENER_NAME = "plain";
    public static final String SCRAMSHA_PLAIN_LISTENER_NAME = "scramplain";
    public static final String SECURE_LISTENER_NAME = "secure";


    // --------------------------------
    // ------------ Console -----------
    // --------------------------------
    /**
     * CRDs
     */
    public static final String CONSOLE_INSTANCE = "console-instance";

    /**
     * OLM
     */
    public static final String CONSOLE_OLM_SUBSCRIPTION_NAME = "console-sub";
}
