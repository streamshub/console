package com.github.streamshub.systemtests.constants;

public interface ResourceKinds {
    // ----------
    // K8s
    // ----------
    String CUSTOM_RESOURCE_DEFINITION = "CustomResourceDefinition";
    String CONFIG_MAP = "ConfigMap";
    String DEPLOYMENT = "Deployment";
    String DEPLOYMENT_CONFIG = "DeploymentConfig";
    String POD = "Pod";
    String SERVICE = "Service";
    String SECRET = "Secret";
    String CLUSTER_ROLE = "ClusterRole";
    String CLUSTER_ROLE_BINDING = "ClusterRoleBinding";
    String ROLE_BINDING = "RoleBinding";
    String SERVICE_ACCOUNT = "ServiceAccount";
    String JOB = "Job";
    String NAMESPACE = "Namespace";
    // ----------
    // Openshift
    // ----------
    String CATALOG_SOURCE = "CatalogSource";
    String SUBSCRIPTION = "Subscription";
    String INSTALL_PLAN = "InstallPlan";
    String CLUSTER_SERVICE_VERSION = "ClusterServiceVersion";
    String OPERATOR_GROUP = "OperatorGroup";
    String OPERATOR = "Operator";
    // ----------
    // Kafka
    // ----------
    String KAFKA_TOPIC = "KafkaTopic";
    String KAFKA = "Kafka";
    String KAFKA_USER = "KafkaUser";
    String KAFKA_NODE_POOL = "KafkaNodePool";
    // ----------
    // Console
    // ----------
    String CONSOLE = "Console";
}
