package com.github.streamshub.systemtests.constants;

import com.github.streamshub.console.api.v1alpha1.Console;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class Constants {
    private Constants() {}

    // --------------------------------
    // ------------ General -----------
    // --------------------------------
    /**
     * Namespaces
     */
    public static final String CO_NAMESPACE = "co-namespace";
    public static final String OPENSHIFT_MARKETPLACE_NAMESPACE = "openshift-marketplace";

    /**
     * Test values
     */
    public static final String USER_USERNAME = "Anonymous";

    /**
     * Messages
     */
    public static final int MESSAGE_COUNT = 1000;

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
    public static final String KAFKA_USER_PREFIX = "user";
    public static final String KAFKA_CLUSTER_PREFIX = "kc";
    public static final String BROKER_ROLE_PREFIX = "brk";
    public static final String CONTROLLER_ROLE_PREFIX = "ctrl";
    public static final int REGULAR_BROKER_REPLICAS = 3;


     // --------------------------------
    // ------------ Console -----------
    // --------------------------------
    /**
     * CRDs
     */
    public static final String CONSOLE_CRD_API_NAME = HasMetadata.getFullResourceName(Console.class);
    public static final String CONSOLE_CRD_API_VERSION = HasMetadata.getGroup(Console.class) + "/" + HasMetadata.getVersion(Console.class);
    public static final String CONSOLE_OPERATOR_GROUP_NAME = "streamshub-operators";
    public static final String CONSOLE_INSTANCE = "console-instance";
}
