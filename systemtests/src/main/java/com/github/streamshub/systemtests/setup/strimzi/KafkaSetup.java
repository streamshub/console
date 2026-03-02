package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ExampleFiles;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.common.template.ContainerEnvVarBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBroker;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceBuilder;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.acl.AclOperation;
import io.strimzi.api.kafka.model.user.acl.AclResourcePatternType;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class KafkaSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaSetup.class);

    private KafkaSetup() {}

    public static void setupDefaultKafkaIfNeeded(String namespaceName, String clusterName) {
        setupKafkaIfNeeded(
            getDefaultKafkaConfigMap(namespaceName, clusterName).build(),
            getDefaultBrokerNodePools(namespaceName, clusterName, Constants.REGULAR_BROKER_REPLICAS).build(),
            getDefaultControllerNodePools(namespaceName, clusterName, Constants.REGULAR_CONTROLLER_REPLICAS).build(),
            getDefaultKafkaUser(namespaceName, clusterName).build(),
            getDefaultKafka(namespaceName, clusterName, Environment.ST_KAFKA_VERSION, Constants.REGULAR_BROKER_REPLICAS).build()
        );
    }

    public static void setupKafkaWithCcIfNeeded(String namespaceName, String clusterName) {
        setupKafkaIfNeeded(
            getDefaultKafkaConfigMap(namespaceName, clusterName).build(),
            getDefaultBrokerNodePools(namespaceName, clusterName, Constants.REGULAR_BROKER_REPLICAS).build(),
            getDefaultControllerNodePools(namespaceName, clusterName, Constants.REGULAR_CONTROLLER_REPLICAS).build(),
            getDefaultKafkaUser(namespaceName, clusterName).build(),
            getKafkaWithCc(namespaceName, clusterName, Environment.ST_KAFKA_VERSION, Constants.REGULAR_BROKER_REPLICAS).build()
        );
    }

    /**
     * Sets up Kafka and its related components if the Kafka cluster does not already exist.
     *
     * <p>This method checks if the Kafka resource is already present in the cluster by namespace
     * and name. If not present, it creates the provided ConfigMap, broker and controller node pools,
     * Kafka resource, and KafkaUser resources sequentially, waiting for each creation to complete.
     * It also waits for the KafkaUser's Secret to become ready before returning.
     *
     * <p>If the Kafka cluster already exists, the method logs a warning and skips deployment.
     *
     * @param configMap the ConfigMap resource for Kafka configuration
     * @param brokerNodePools the KafkaNodePool resource defining the broker node pools
     * @param controllerNodePools the KafkaNodePool resource defining the controller node pools
     * @param kafkaUser the KafkaUser resource representing Kafka user credentials
     * @param kafka the Kafka custom resource representing the Kafka cluster
     */
    public static void setupKafkaIfNeeded(ConfigMap configMap, KafkaNodePool brokerNodePools, KafkaNodePool controllerNodePools, KafkaUser kafkaUser, Kafka kafka) {
        LOGGER.info("Setup test Kafka {}/{}  and it's components", kafka.getMetadata().getNamespace(), kafka.getMetadata().getName());
        if (ResourceUtils.getKubeResource(Kafka.class, kafka.getMetadata().getNamespace(), kafka.getMetadata().getName()) == null) {
            KubeResourceManager.get().createResourceWithWait(configMap);
            KubeResourceManager.get().createResourceWithWait(brokerNodePools);
            KubeResourceManager.get().createResourceWithWait(controllerNodePools);
            KubeResourceManager.get().createResourceWithWait(kafka);
            KubeResourceManager.get().createResourceWithWait(kafkaUser);
            WaitUtils.waitForSecretReady(kafkaUser.getMetadata().getNamespace(), kafkaUser.getMetadata().getName());
        }  else {
            LOGGER.warn("Skipping Kafka deployment, there already is a Kafka cluster");
        }
    }

    /**
     * Returns the default Kafka metrics {@link ConfigMapBuilder} for the specified namespace and cluster.
     *
     * <p>The ConfigMap is created by loading a predefined YAML template and customizing its metadata
     * with the provided namespace, cluster name, and appropriate labels.
     *
     * @param namespaceName the Kubernetes namespace where the ConfigMap will be created
     * @param clusterName the name of the Kafka cluster
     * @return a {@link ConfigMapBuilder} configured with default Kafka metrics settings
     */
    public static ConfigMapBuilder getDefaultKafkaConfigMap(String namespaceName, String clusterName) {
        return new ConfigMapBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLES_KAFKA_METRICS_CONFIG_MAP, ConfigMap.class))
            .editMetadata()
                .withName(KafkaNamingUtils.kafkaMetricsConfigMapName(clusterName))
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata();
    }

    /**
     * Creates the default {@link KafkaNodePoolBuilder} for Kafka brokers with the specified configuration.
     *
     * <p>The node pool is configured with the given namespace, cluster name, number of replicas,
     * broker role, and JBOD persistent storage of 1Gi per broker with claim deletion enabled.
     *
     * @param namespaceName the Kubernetes namespace for the KafkaNodePool resource
     * @param clusterName the name of the Kafka cluster
     * @param replicas the number of broker replicas to configure in the node pool
     * @return a {@link KafkaNodePoolBuilder} configured as a broker node pool with JBOD storage
     */
    public static KafkaNodePoolBuilder getDefaultBrokerNodePools(String namespaceName, String clusterName, int replicas) {
        return new KafkaNodePoolBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KafkaNamingUtils.brokerPoolName(clusterName))
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .withNewSpec()
                .withReplicas(replicas)
                .withRoles(ProcessRoles.BROKER)
                .withNewJbodStorage()
                    .addNewPersistentClaimStorageVolume()
                        .withId(0)
                        .withDeleteClaim(true)
                        .withSize("1Gi")
                    .endPersistentClaimStorageVolume()
                .endJbodStorage()
            .endSpec();
    }

    /**
     * Creates the default {@link KafkaNodePoolBuilder} for Kafka controllers with the specified configuration.
     *
     * <p>The node pool is configured with the given namespace, cluster name, number of replicas,
     * controller role, and JBOD persistent storage of 1Gi per controller with claim deletion enabled.
     *
     * @param namespaceName the Kubernetes namespace for the KafkaNodePool resource
     * @param clusterName the name of the Kafka cluster
     * @param replicas the number of controller replicas to configure in the node pool
     * @return a {@link KafkaNodePoolBuilder} configured as a controller node pool with JBOD storage
     */
    public static KafkaNodePoolBuilder getDefaultControllerNodePools(String namespaceName, String clusterName, int replicas) {
        return new KafkaNodePoolBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KafkaNamingUtils.controllerPoolName(clusterName))
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .withNewSpec()
                .withReplicas(replicas)
                .withRoles(ProcessRoles.CONTROLLER)
                .withNewJbodStorage()
                    .addNewPersistentClaimStorageVolume()
                        .withId(0)
                        .withDeleteClaim(true)
                        .withSize("1Gi")
                    .endPersistentClaimStorageVolume()
                .endJbodStorage()
            .endSpec();
    }

    /**
     * Creates the default {@link KafkaUserBuilder} for the given Kafka cluster namespace and name.
     *
     * <p>The KafkaUser is configured with SCRAM-SHA-512 client authentication and simple authorization
     * rules granting permissions to describe cluster resources, read and describe groups, and
     * read and describe topics with literal resource patterns.
     *
     * @param namespaceName the Kubernetes namespace for the KafkaUser resource
     * @param clusterName the name of the Kafka cluster to associate the user with
     * @return a {@link KafkaUserBuilder} configured with default SCRAM-SHA-512 authentication and authorization rules
     */
    public static KafkaUserBuilder getDefaultKafkaUser(String namespaceName, String clusterName) {
        return new KafkaUserBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KafkaNamingUtils.kafkaUserName(clusterName))
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .withNewSpec()
                .withNewKafkaUserScramSha512ClientAuthentication()
                .endKafkaUserScramSha512ClientAuthentication()
            .withNewKafkaUserAuthorizationSimple()
                .addNewAcl()
                    .withNewAclRuleClusterResource()
                    .endAclRuleClusterResource()
                    .withOperations(AclOperation.ALL)
                .endAcl()
                .addNewAcl()
                    .withNewAclRuleGroupResource()
                        .withName("*")
                        .withPatternType(AclResourcePatternType.LITERAL)
                    .endAclRuleGroupResource()
                    .withOperations(AclOperation.ALL)
                .endAcl()
                .addNewAcl()
                    .withNewAclRuleTopicResource()
                        .withName("*")
                        .withPatternType(AclResourcePatternType.LITERAL)
                    .endAclRuleTopicResource()
                    .withOperations(AclOperation.ALL)
                .endAcl()
            .endKafkaUserAuthorizationSimple()
            .endSpec();
    }

    /**
     * Creates a default {@link KafkaBuilder} custom resource configured with standard settings.
     *
     * <p>This method configures the Kafka cluster with the specified namespace, cluster name,
     * Kafka version, and number of replicas. It enables KRaft mode and node pools via annotations,
     * sets up entity operators (user and topic operators), and configures Kafka listeners
     * including a plain listener and a secure listener with SCRAM-SHA-512 authentication.
     *
     * <p>Replication factors and in-sync replica settings are automatically calculated
     * based on the number of replicas, capped by predefined limits.
     *
     * <p>The secure listener type depends on whether the cluster is running on OpenShift
     * (Route) or not (Ingress), and sets up bootstrap and broker hostnames accordingly.
     *
     * <p>A JMX Prometheus Exporter metrics configuration is included, referencing a ConfigMap
     * for metrics scraping.
     *
     * @param namespaceName the Kubernetes namespace where the Kafka cluster will be deployed
     * @param clusterName the name of the Kafka cluster resource
     * @param kafkaVersion the Kafka version to use (e.g., "3.5.0")
     * @param replicas the number of Kafka broker replicas to configure
     * @return a fully built {@link KafkaBuilder} resource object with the default configuration
     */
    public static KafkaBuilder getDefaultKafka(String namespaceName, String clusterName, String kafkaVersion, int replicas) {
        // This helps to avoid issues with same-name kafka in different namespace exposing the same hostname
        String hashedNamespace = Utils.hashStub(namespaceName);

        // Set broker hosts dynamically
        List<GenericKafkaListenerConfigurationBroker> brokerHosts = IntStream.range(0, replicas)
            .mapToObj(id -> new GenericKafkaListenerConfigurationBrokerBuilder()
                .withBroker(id)
                .withHost(String.join(".", "broker-" + id, hashedNamespace, clusterName, ClusterUtils.getClusterDomain()))
                .build())
            .toList();

        return new KafkaBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .editMetadata()
                .withNamespace(namespaceName)
                .withName(clusterName)
            .endMetadata()
            .editSpec()
                .withNewEntityOperator()
                    .withNewUserOperator()
                    .endUserOperator()
                    .withNewTopicOperator()
                        .withReconciliationIntervalMs(20_000L)
                    .endTopicOperator()
                .withNewTemplate()
                    .withNewTopicOperatorContainer()
                        .withEnv(new ContainerEnvVarBuilder()
                            .withName("STRIMZI_USE_FINALIZERS")
                            .withValue("false")
                            .build())
                    .endTopicOperatorContainer()
                .endTemplate()
                .endEntityOperator()
                .editKafka()
                    .withVersion(kafkaVersion)
                    .withNewKafkaAuthorizationSimple()
                    .endKafkaAuthorizationSimple()
                    .addToConfig("offsets.topic.replication.factor", Math.min(replicas, 3))
                    .addToConfig("transaction.state.log.min.isr", Math.min(replicas, 2))
                    .addToConfig("transaction.state.log.replication.factor", Math.min(replicas, 3))
                    .addToConfig("default.replication.factor", Math.min(replicas, 3))
                    .addToConfig("min.insync.replicas", Math.min(Math.max(replicas - 1, 1), 2))
                    .addToListeners(new GenericKafkaListenerBuilder()
                        .withName(Constants.PLAIN_LISTENER_NAME)
                        .withPort(9092)
                        .withType(KafkaListenerType.INTERNAL)
                        .withTls(false)
                        .build())
                    .addToListeners(new GenericKafkaListenerBuilder()
                        .withName(Constants.SCRAMSHA_PLAIN_LISTENER_NAME)
                        .withPort(9095)
                        .withType(KafkaListenerType.INTERNAL)
                        .withTls(false)
                        .withNewKafkaListenerAuthenticationScramSha512Auth()
                        .endKafkaListenerAuthenticationScramSha512Auth()
                        .build())
                    .addToListeners(new GenericKafkaListenerBuilder()
                        .withName(Constants.SECURE_LISTENER_NAME)
                        .withPort(9093)
                        .withTls(true)
                        .withType(ClusterUtils.isOcp() ? KafkaListenerType.ROUTE : KafkaListenerType.INGRESS)
                        .withNewKafkaListenerAuthenticationScramSha512Auth()
                        .endKafkaListenerAuthenticationScramSha512Auth()
                        .withNewConfiguration()
                            .withNewBootstrap()
                                .withHost(String.join(".", "bootstrap", hashedNamespace, clusterName, ClusterUtils.getClusterDomain()))
                            .endBootstrap()
                            .withBrokers(brokerHosts)
                        .endConfiguration()
                        .build())
                    .withNewJmxPrometheusExporterMetricsConfig()
                        .withNewValueFrom()
                            .withNewConfigMapKeyRef("kafka-metrics-config.yml", clusterName + "-metrics", false)
                        .endValueFrom()
                    .endJmxPrometheusExporterMetricsConfig()
                .endKafka()
            .endSpec();
    }


    public static KafkaBuilder getKafkaWithCc(String namespaceName, String clusterName, String kafkaVersion, int replicas) {
        // contains fastest reliable config for CC
        return getDefaultKafka(namespaceName, clusterName, kafkaVersion, replicas).editSpec()
            .editKafka()
                .addToConfig("cruise.control.metrics.reporter.metrics.reporting.interval.ms", 5_000)
                .addToConfig("cruise.control.metrics.reporter.metadata.max.age.ms", 4_000)
            .endKafka()
            .editOrNewCruiseControl()
                .addToConfig("max.active.user.tasks", 10)
                .addToConfig("sample.store.topic.replication.factor", 1)
                .addToConfig("broker.sample.store.topic.partition.count", 1)
                .addToConfig("metadata.max.age.ms", 4_000)
                .addToConfig("metric.sampling.interval.ms", 5_000)
                .addToConfig("cruise.control.metrics.reporter.metrics.reporting.interval.ms", 5_000)
                .addToConfig("skip.sample.store.topic.rack.awareness.check", true)
            .endCruiseControl()
            .endSpec();
    }


    public static KafkaRebalanceBuilder getKafkaRebalance(String namespace, String clusterName, String rebalanceName) {
        return new KafkaRebalanceBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName)
                .withNamespace(namespace)
                .withName(rebalanceName)
            .endMetadata()
            .withNewSpec()
            .endSpec();
    }
}
