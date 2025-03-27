package com.github.streamshub.systemtests.constants;

public class ResourceKinds {
    private ResourceKinds() {}

    // ----------
    // K8s
    // ----------
    public static final String CUSTOM_RESOURCE_DEFINITION = "CustomResourceDefinition";
    public static final String CONFIG_MAP = "ConfigMap";
    public static final String DEPLOYMENT = "Deployment";
    public static final String DEPLOYMENT_CONFIG = "DeploymentConfig";
    public static final String POD = "Pod";
    public static final String SERVICE = "Service";
    public static final String SECRET = "Secret";
    public static final String CLUSTER_ROLE = "ClusterRole";
    public static final String CLUSTER_ROLE_BINDING = "ClusterRoleBinding";
    public static final String ROLE_BINDING = "RoleBinding";
    public static final String SERVICE_ACCOUNT = "ServiceAccount";
    public static final String JOB = "Job";
    public static final String NAMESPACE = "Namespace";

    // ----------
    // OpenShift
    // ----------
    public static final String CATALOG_SOURCE = "CatalogSource";
    public static final String SUBSCRIPTION = "Subscription";
    public static final String INSTALL_PLAN = "InstallPlan";
    public static final String CLUSTER_SERVICE_VERSION = "ClusterServiceVersion";
    public static final String OPERATOR_GROUP = "OperatorGroup";
    public static final String OPERATOR = "Operator";

    // ----------
    // Kafka
    // ----------
    public static final String KAFKA_TOPIC = "KafkaTopic";
    public static final String KAFKA = "Kafka";
    public static final String KAFKA_USER = "KafkaUser";
    public static final String KAFKA_NODE_POOL = "KafkaNodePool";

    // ----------
    // Console
    // ----------
    public static final String CONSOLE = "Console";

}
