package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ExampleFiles;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.KafkaUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class KafkaSetupConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaSetupConfig.class);

    private final String namespaceName;

    // Cluster
    private final String clusterName;
    private final String brokerPoolName;
    private final String controllerPoolName;
    private final String kafkaVersion;
    private final int replicas;
    // User
    private final String kafkaUsername;
    // Topic
    private final String topicPrefixName;
    private final String producerName;
    private final String consumerName;

    public KafkaSetupConfig(String namespaceName) {
        this(namespaceName, KafkaUtils.kafkaClusterName(namespaceName));
    }

    public KafkaSetupConfig(String namespaceName, String kafkaClusterName) {
        this.namespaceName = namespaceName;
        // Cluster
        this.clusterName = kafkaClusterName;
        this.brokerPoolName = KafkaUtils.brokerPoolName(kafkaClusterName);
        this.controllerPoolName = KafkaUtils.controllerPoolName(kafkaClusterName);
        this.kafkaVersion = Environment.ST_KAFKA_VERSION;
        this.replicas = Constants.REGULAR_BROKER_REPLICAS;
        // User
        this.kafkaUsername = KafkaUtils.kafkaUserName(kafkaClusterName);
        // Topic
        this.topicPrefixName = Constants.KAFKA_TOPIC_PREFIX + "-" + Utils.hashStub(kafkaClusterName);
        this.producerName = topicPrefixName + "-" + Constants.PRODUCER;
        this.consumerName = topicPrefixName + "-" + Constants.CONSUMER;
    }

    public void setupIfNeeded() {
        LOGGER.info("Deploy test Kafka cluster {}/{}", namespaceName, clusterName);

        if (ResourceUtils.getKubeResource(ConfigMap.class, namespaceName, KafkaUtils.kafkaMetricsConfigMapName(clusterName)) == null) {
            KubeResourceManager.get().createResourceWithWait(getKafkaConfigMapFromExamples());
        }

        if (ResourceUtils.getKubeResource(Kafka.class, namespaceName, clusterName) == null) {
            KubeResourceManager.get().createResourceWithWait(getBrokerNodePoolsFromExamples());
            KubeResourceManager.get().createResourceWithWait(getControllerNodePoolsFromExamples());

            KubeResourceManager.get().createResourceWithWait(getKafkaFromExamples());

            KubeResourceManager.get().createResourceWithWait(getKafkaUserFromExamples());
            WaitUtils.waitForSecretReady(namespaceName, kafkaUsername);
        }  else {
            LOGGER.warn("Skipping Kafka deployment, there already is a Kafka cluster");
        }
    }

    // ----------
    // Getters
    // ----------
    public String getClusterName() {
        return clusterName;
    }

    private ConfigMap getKafkaConfigMapFromExamples() {
        return new ConfigMapBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLES_KAFKA_METRICS_CONFIG_MAP, ConfigMap.class))
            .editMetadata()
                .withName(KafkaUtils.kafkaMetricsConfigMapName(clusterName))
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .build();
    }

    private KafkaNodePool getBrokerNodePoolsFromExamples() {
        return new KafkaNodePoolBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLES_KAFKA_NODEPOOLS_BROKER, KafkaNodePool.class))
            .editMetadata()
                .withName(brokerPoolName)
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .editSpec()
                .withReplicas(replicas)
            .endSpec()
            .build();
    }

    private KafkaNodePool getControllerNodePoolsFromExamples() {
        return new KafkaNodePoolBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLES_KAFKA_NODEPOOLS_CONTROLLER, KafkaNodePool.class))
            .editMetadata()
                .withName(controllerPoolName)
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .editSpec()
                .withReplicas(replicas)
            .endSpec()
            .build();
    }

    private KafkaUser getKafkaUserFromExamples() {
        return new KafkaUserBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLES_KAFKA_USER, KafkaUser.class))
            .editMetadata()
                .withName(kafkaUsername)
                .withNamespace(namespaceName)
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName))
            .endMetadata()
            .build();
    }

    private Kafka getKafkaFromExamples() {
        String kafkaYaml;
        String listenerType = ClusterUtils.isOcp() ? Route.class.getSimpleName().toLowerCase(Locale.ENGLISH) : Ingress.class.getSimpleName().toLowerCase(Locale.ENGLISH);

        try (InputStream yamlContentStream = ExampleFiles.EXAMPLES_KAFKA.toURI().toURL().openStream()) {
            kafkaYaml = new String(yamlContentStream.readAllBytes(), StandardCharsets.UTF_8)
                .replace("${LISTENER_TYPE}", listenerType)
                .replace("${CLUSTER_DOMAIN}", ClusterUtils.getClusterDomain());
        } catch (IOException e) {
            throw new SetupException("Cannot get example Kafka YAML resource: ", e);
        }

        return new KafkaBuilder(TestFrameUtils.configFromYaml(kafkaYaml, Kafka.class))
            .editMetadata()
                .withNamespace(namespaceName)
                .withName(clusterName)
            .endMetadata()
            .editSpec()
                .editKafka()
                    .withVersion(kafkaVersion)
                    .editFirstListener()
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
                    .endListener()
                    .addToListeners(new GenericKafkaListenerBuilder()
                        .withName(Constants.PLAIN_LISTENER_NAME)
                        .withPort(9092)
                        .withType(KafkaListenerType.INTERNAL)
                        .withTls(false)
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
