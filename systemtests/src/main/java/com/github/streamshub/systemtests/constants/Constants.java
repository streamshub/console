package com.github.streamshub.systemtests.constants;

public class Constants {
    public static final String NGINX_INGRESS_NAMESPACE = "ingress-nginx";

    // As of now for strimzi operator 0.49.0, it is required to convert all resources to `apiVersion: v1`
    // Best way is to hard-set this value when creating strimzi CRs to avoid long upgrading process described in:
    // https://strimzi.io/docs/operators/0.49.0/deploying.html#proc-convert-custom-resources-cluster-str
    public static final String STRIMZI_API_V1 = "v1";

    private Constants() {}

    // --------------------------------
    // ------------ General -----------
    // --------------------------------
    public static final int MAX_ACTION_RETRIES = 10;
    public static final int LOGOUT_RETRIES = 5;
    /**
     * Cluster
     */
    public static final String OPENSHIFT_CONSOLE = "openshift-console";
    public static final String OPENSHIFT_CONSOLE_ROUTE_NAME = "console";
    public static final String DEFAULT_NAMESPACE = "default";
    public static final String PROMETHEUS_NAME = "prometheus";

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
    public static final int DEFAULT_TOPICS_PER_PAGE = 20;

    // Topic Prefixes
    public static final String REPLICATED_TOPICS_PREFIX = "replicated";
    public static final String UNMANAGED_REPLICATED_TOPICS_PREFIX = "unmanaged-replicated";
    public static final String UNDER_REPLICATED_TOPICS_PREFIX = "underreplicated";
    public static final String UNAVAILABLE_TOPICS_PREFIX = "unavailable";

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
    public static final String KAFKA_CONNECT_CLUSTER_PREFIX = "kcc";
    public static final String BROKER_ROLE_PREFIX = "brk";
    public static final String CONTROLLER_ROLE_PREFIX = "ctrl";
    public static final int REGULAR_BROKER_REPLICAS = 3;
    public static final int REGULAR_CONTROLLER_REPLICAS = 3;
    public static final int REGULAR_KAFKA_CONNECT_REPLICAS = 1;

    public static final String CONNECT_BUILD_IMAGE_NAME = "st-connect-build";
    public static final String DEFAULT_SINK_FILE_PATH = "/tmp/test-file-sink.txt";
    public static final int CONNECT_SERVICE_PORT = 8083;

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

    /**
     * Keycloak
     */
    public static final String KEYCLOAK_HOSTNAME_PREFIX = "console-oidc";
    public static final String KEYCLOAK_CLIENT_ID = "console-client";
    public static final String KEYCLOAK_TRUST_STORE_CONFIGMAP_NAME = "truststore-configmap";
    public static final String KEYCLOAK_TRUST_STORE_ACCCESS_SECRET_NAME = "access-to-truststore";
    public static final String PASSWORD_KEY_NAME = "password"; // NOSONAR - test password
    public static final String TRUST_STORE_KEY_NAME = "truststore";

    public static final String KEYCLOAK_REALM = "console-realm";
    public static final String TRUST_STORE_PASSWORD = "changeit"; // NOSONAR - test password

    // --------------------------------
    // ------------ Apicurio ----------
    // --------------------------------
    public static final String APICURIO_PREFIX = "apicurio";
    public static final String APICURIO_API_V3_SUFFIX = "/apis/registry/v3";
    public static final String APICURIO_VALUE_GROUP_ID = "apicurio.value.groupId";
    public static final String APICURIO_VALUE_ARTIFACT_ID = "apicurio.value.artifactId";
    public static final String APICURIO_DEFAULT_GROUP = "default";
    // Artifact
    public static final String ARTIFACT_TYPE_PROTOBUF = "protobuf";
    public static final String ARTIFACT_TYPE_AVRO = "avro";
    public static final String ARTIFACT_TYPE_JSON = "json";
    public static final String ARTIFACT_TYPE_XML = "xml";
    public static final String PLAIN_VALUE_TYPE = "Plain";
    // Content type
    public static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    // Serializers
    public static final String SERIALIZER_PROTOBUF = "io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer";
    public static final String SERIALIZER_AVRO = "io.apicurio.registry.serde.avro.AvroKafkaSerializer";
    public static final String SERIALIZER_JSON = "io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer";
}
