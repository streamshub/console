package com.github.streamshub.systemtests.constants;

public class ResourceKinds {
    private ResourceKinds() {}

    // ----------
    // K8s
    // ----------
    public static final String CUSTOM_RESOURCE_DEFINITION = "customresourcedefinition";
    public static final String CONFIG_MAP = "configmap";
    public static final String DEPLOYMENT = "deployment";
    public static final String DEPLOYMENT_CONFIG = "deploymentconfig";
    public static final String POD = "pod";
    public static final String SERVICE = "service";
    public static final String SECRET = "secret";
    public static final String CLUSTER_ROLE = "clusterrole";
    public static final String CLUSTER_ROLE_BINDING = "clusterrolebinding";
    public static final String ROLE_BINDING = "rolebinding";
    public static final String SERVICE_ACCOUNT = "serviceaccount";
    public static final String JOB = "job";
    public static final String NAMESPACE = "namespace";
    // ----------
    // Openshift
    // ----------
    public static final String CATALOG_SOURCE = "catalogsource";
    public static final String SUBSCRIPTION = "subscription";
    public static final String INSTALL_PLAN = "installplan";
    public static final String CLUSTER_SERVICE_VERSION = "clusterserviceversion";
    public static final String OPERATOR_GROUP = "operatorgroup";
    public static final String OPERATOR = "operator";
    // ----------
    // Kafka
    // ----------
    public static final String KAFKA_TOPIC = "kafkatopic";
    public static final String KAFKA = "kafka";
    public static final String KAFKA_USER = "kafkauser";
    public static final String KAFKA_NODE_POOL = "kafkanodepool";
    // ----------
    // Console
    // ----------
    public static final String CONSOLE = "console";
}
