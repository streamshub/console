package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ExampleFiles;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.acl.AclOperation;
import io.strimzi.api.kafka.model.user.acl.AclResourcePatternType;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class KafkaSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaSetup.class);

    private KafkaSetup() {}

    public static void setupDefaultKafkaIfNeeded(String namespaceName, String clusterName) {
        setupKafkaIfNeeded(
            getDefaultKafkaConfigMap(namespaceName, clusterName),
            getDefaultBrokerNodePools(namespaceName, clusterName, Constants.REGULAR_BROKER_REPLICAS),
            getDefaultControllerNodePools(namespaceName, clusterName, Constants.REGULAR_BROKER_REPLICAS),
            getDefaultKafkaUser(namespaceName, clusterName),
            getDefaultKafka(namespaceName, clusterName, Environment.ST_KAFKA_VERSION, Constants.REGULAR_BROKER_REPLICAS)
        );
    }

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

    public static ConfigMap getDefaultKafkaConfigMap(String namespaceName, String clusterName) {
        return new ConfigMapBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLES_KAFKA_METRICS_CONFIG_MAP, ConfigMap.class))
            .editMetadata()
                .withName(KafkaNamingUtils.kafkaMetricsConfigMapName(clusterName))
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .build();
    }

    public static KafkaNodePool getDefaultBrokerNodePools(String namespaceName, String clusterName, int replicas) {
        return new KafkaNodePoolBuilder()
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
                        .withSize("10Gi")
                    .endPersistentClaimStorageVolume()
                .endJbodStorage()
            .endSpec()
            .build();
    }

    public static KafkaNodePool getDefaultControllerNodePools(String namespaceName, String clusterName, int replicas) {
        return new KafkaNodePoolBuilder()
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
                        .withSize("10Gi")
                    .endPersistentClaimStorageVolume()
                .endJbodStorage()
            .endSpec()
            .build();
    }

    public static KafkaUser getDefaultKafkaUser(String namespaceName, String clusterName) {
        return new KafkaUserBuilder()
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
                    .withOperations(AclOperation.DESCRIBE, AclOperation.DESCRIBECONFIGS)
                .endAcl()
                .addNewAcl()
                    .withNewAclRuleGroupResource()
                        .withName("*")
                        .withPatternType(AclResourcePatternType.LITERAL)
                    .endAclRuleGroupResource()
                    .withOperations(AclOperation.READ, AclOperation.DESCRIBE)
                .endAcl()
                .addNewAcl()
                    .withNewAclRuleTopicResource()
                        .withName("*")
                        .withPatternType(AclResourcePatternType.LITERAL)
                    .endAclRuleTopicResource()
                    .withOperations(AclOperation.READ, AclOperation.DESCRIBE, AclOperation.DESCRIBECONFIGS)
                .endAcl()
            .endKafkaUserAuthorizationSimple()
            .endSpec()
            .build();
    }

    public static Kafka getDefaultKafka(String namespaceName, String clusterName, String kafkaVersion, int replicas) {
        return new KafkaBuilder()
            .editMetadata()
                .withNamespace(namespaceName)
                .withName(clusterName)
                .addToAnnotations(ResourceAnnotations.ANNO_STRIMZI_IO_KRAFT, "enabled")
                .addToAnnotations(ResourceAnnotations.ANNO_STRIMZI_IO_NODE_POOLS, "enabled")
            .endMetadata()
            .editSpec()
                .withNewEntityOperator()
                    .withNewUserOperator()
                    .endUserOperator()
                    .withNewTopicOperator()
                    .endTopicOperator()
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
                    .addToConfig("allow.everyone.if.no.acl.found", "true")
                    .addToListeners(new GenericKafkaListenerBuilder()
                        .withName(Constants.PLAIN_LISTENER_NAME)
                        .withPort(9092)
                        .withType(KafkaListenerType.INTERNAL)
                        .withTls(false)
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
                                .withHost(String.join(".", "bootstrap", clusterName, ClusterUtils.getClusterDomain()))
                            .endBootstrap()
                            .withBrokers(
                                new GenericKafkaListenerConfigurationBrokerBuilder()
                                    .withBroker(0)
                                    .withHost(String.join(".", "broker-0", clusterName, ClusterUtils.getClusterDomain()))
                                .build(),
                                new GenericKafkaListenerConfigurationBrokerBuilder()
                                    .withBroker(1)
                                    .withHost(String.join(".", "broker-1", clusterName, ClusterUtils.getClusterDomain()))
                                .build(),
                                new GenericKafkaListenerConfigurationBrokerBuilder()
                                    .withBroker(2)
                                    .withHost(String.join(".", "broker-2", clusterName, ClusterUtils.getClusterDomain()))
                                .build())
                        .endConfiguration()
                        .build())
                    .withNewJmxPrometheusExporterMetricsConfig()
                        .withNewValueFrom()
                            .withNewConfigMapKeyRef("kafka-metrics-config.yml", clusterName + "-metrics", false)
                        .endValueFrom()
                    .endJmxPrometheusExporterMetricsConfig()
                .endKafka()
            .endSpec()
            .build();
    }
}
