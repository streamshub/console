package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.connect.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.connect.KafkaConnectResources;
import io.strimzi.api.kafka.model.connect.build.DockerOutput;
import io.strimzi.api.kafka.model.connect.build.DockerOutputBuilder;
import io.strimzi.api.kafka.model.connect.build.JarArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.Plugin;
import io.strimzi.api.kafka.model.connect.build.PluginBuilder;
import io.strimzi.api.kafka.model.connector.KafkaConnectorBuilder;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class KafkaConnectSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaConnectSetup.class);

    private KafkaConnectSetup() {}

    public static void setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(String namespace, String connectName, String kafkaName, String kafkaUserName) {
        LOGGER.info("Setup test Kafka {}/{}  and it's components", namespace, connectName);
        if (ResourceUtils.getKubeResource(KafkaConnect.class, namespace, connectName) == null) {
            KubeResourceManager.get().createResourceWithWait(
                kafkaDefaultConnectWithFilePlugin(namespace, connectName, kafkaName, kafkaUserName, Constants.REGULAR_KAFKA_CONNECT_REPLICAS)
                .build());
        }  else {
            LOGGER.warn("Skipping Kafka deployment, there already is a Kafka cluster");
        }
    }

    private static KafkaConnectBuilder getDefaultKafkaConnectWithScramShaUser(String namespace, String connectName, String kafkaName, String kafkaUserName, int replicas) {
        return new KafkaConnectBuilder()
            .withNewMetadata()
                .withName(connectName)
                .withNamespace(namespace)
            .endMetadata()
            .editOrNewSpec()
                .withVersion(Environment.ST_KAFKA_VERSION)
                .withBootstrapServers(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
                .withGroupId(KafkaConnectResources.componentName(connectName))
                .withConfigStorageTopic(KafkaConnectResources.configMapName(connectName))
                .withOffsetStorageTopic(KafkaConnectResources.configStorageTopicOffsets(connectName))
                .withStatusStorageTopic(KafkaConnectResources.configStorageTopicStatus(connectName))
                .withReplicas(replicas)
                .withNewKafkaClientAuthenticationScramSha256()
                    .withNewPasswordSecret()
                        .withPassword("password")
                        .withSecretName(kafkaUserName)
                    .endPasswordSecret()
                    .withUsername(kafkaUserName)
                .endKafkaClientAuthenticationScramSha256()
                .addToConfig("config.storage.replication.factor", "-1")
                .addToConfig("offset.storage.replication.factor", "-1")
                .addToConfig("status.storage.replication.factor", "-1")
                .withNewInlineLogging()
                    .addToLoggers("rootLogger.level", "DEBUG")
                .endInlineLogging()
            .endSpec();
    }

    public static KafkaConnectBuilder kafkaDefaultConnectWithFilePlugin(String namespace, String connectName, String kafkaName, String kafkaUser, int replicas) {
        return addFileSinkPluginOrImage(namespace, getDefaultKafkaConnectWithScramShaUser(namespace, connectName, kafkaName, kafkaUser, replicas));
    }

    public static KafkaConnectBuilder addFileSinkPluginOrImage(String namespace, KafkaConnectBuilder kafkaConnectBuilder) {
        if (Environment.CONNECT_IMAGE_WITH_FILE_SINK_PLUGIN.isEmpty()) {
            final Plugin fileSinkPlugin = new PluginBuilder()
                .withName("file-plugin")
                .withArtifacts(
                    new JarArtifactBuilder()
                        .withUrl(Environment.ST_FILE_PLUGIN_URL)
                        .build()
                )
                .build();

            final String imageFullPath = Environment.getImageOutputRegistry(namespace, Constants.CONNECT_BUILD_IMAGE_NAME, String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));

            return kafkaConnectBuilder
                .editOrNewSpec()
                    .editOrNewBuild()
                        .withPlugins(fileSinkPlugin)
                        .withOutput(dockerOutput(imageFullPath))
                    .endBuild()
                .endSpec();
        } else {

            LOGGER.info("Using {} image from {} env variable", Environment.CONNECT_IMAGE_WITH_FILE_SINK_PLUGIN, Environment.CONNECT_IMAGE_WITH_FILE_SINK_PLUGIN);

            return kafkaConnectBuilder
                .editOrNewSpec()
                    .withImage(Environment.CONNECT_IMAGE_WITH_FILE_SINK_PLUGIN)
                .endSpec();
        }
    }

    private static DockerOutput dockerOutput(String imageName) {
        DockerOutputBuilder dockerOutputBuilder = new DockerOutputBuilder().withImage(imageName);
        if (Environment.CONNECT_BUILD_REGISTRY_SECRET != null && !Environment.CONNECT_BUILD_REGISTRY_SECRET.isEmpty()) {
            dockerOutputBuilder.withPushSecret(Environment.CONNECT_BUILD_REGISTRY_SECRET);
        }

        return dockerOutputBuilder.build();
    }


    public static KafkaConnectorBuilder defaultKafkaConnector(String namespaceName, String connectorName, String connectName, int maxTasks) {
        return new KafkaConnectorBuilder()
            .withNewMetadata()
                .withName(connectorName)
                .withNamespace(namespaceName)
                .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, connectName)
            .endMetadata()
            .editOrNewSpec()
                .withTasksMax(maxTasks)
                .withClassName("org.apache.kafka.connect.file.FileStreamSourceConnector")
                .addToConfig("file", "/opt/kafka/LICENSE")
            .endSpec();
    }

    public static void allowConnectConsoleNetworkPolicy(String namespace, String consoleInstanceName, String connectName) {
        LOGGER.info("Apply NetworkPolicy for connect as it's disabled by default");

        NetworkPolicy networkPolicy = new NetworkPolicyBuilder()
            .withNewMetadata()
                .withName("console-connect-allow")
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withNewPodSelector()
                    .addToMatchLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, connectName)
                    .addToMatchLabels(ResourceLabels.STRIMZI_KIND_LABEL, KafkaConnect.RESOURCE_KIND)
                .endPodSelector()
                .addNewIngress()
                    .addNewFrom()
                        .withNewPodSelector()
                            .addToMatchLabels(Labels.CONSOLE_INSTANCE_LABEL, ConsoleUtils.getConsoleDeploymentName(consoleInstanceName))
                        .endPodSelector()
                    .endFrom()
                    .addNewPort()
                        .withNewPort(Constants.CONNECT_SERVICE_PORT)
                        .withProtocol("TCP")
                    .endPort()
                .endIngress()
            .endSpec()
            .build();

        KubeResourceManager.get().createResourceWithWait(networkPolicy);
    }
}
