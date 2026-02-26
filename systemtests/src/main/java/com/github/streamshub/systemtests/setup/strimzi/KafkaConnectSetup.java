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

import java.util.UUID;

public class KafkaConnectSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaConnectSetup.class);

    private KafkaConnectSetup() {}

    /**
     * Ensures that a default {@code KafkaConnect} cluster with the File plugin is deployed
     * in the given namespace. If the resource does not already exist, it is created and
     * configured for use with the Console instance.
     *
     * <p>The method performs the following steps:
     * <ul>
     *   <li>Checks whether a {@code KafkaConnect} resource with the given name exists.</li>
     *   <li>If it does not exist:
     *     <ul>
     *       <li>Creates a default Kafka Connect deployment with the File plugin enabled.</li>
     *       <li>Waits for the resource to become ready.</li>
     *       <li>Configures the necessary NetworkPolicy to allow communication
     *           between the Console instance and the Kafka Connect cluster.</li>
     *     </ul>
     *   </li>
     *   <li>If it already exists, logs a warning and skips creation.</li>
     * </ul>
     *
     * <p>This method is intended primarily for test environment setup to ensure
     * a predictable and reusable Kafka Connect configuration.
     *
     * @param namespace           the Kubernetes namespace where the Kafka Connect cluster should exist
     * @param connectName         the name of the Kafka Connect cluster
     * @param kafkaName           the name of the associated Kafka cluster
     * @param kafkaUserName       the Kafka user used for authentication
     * @param consoleInstanceName the name of the Console instance requiring network access
     */
    public static void setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(String namespace, String connectName, String kafkaName, String kafkaUserName, String consoleInstanceName) {
        LOGGER.info("Setup test Kafka {}/{}  and it's components", namespace, connectName);
        if (ResourceUtils.getKubeResource(KafkaConnect.class, namespace, connectName) == null) {
            KubeResourceManager.get().createResourceWithWait(
                kafkaDefaultConnectWithFilePlugin(namespace, connectName, kafkaName, kafkaUserName, Constants.REGULAR_KAFKA_CONNECT_REPLICAS)
                .build());
            KafkaConnectSetup.allowConnectConsoleNetworkPolicy(namespace, consoleInstanceName, connectName);
        }  else {
            LOGGER.warn("Skipping Kafka deployment, there already is a Kafka cluster");
        }
    }

    private static KafkaConnectBuilder getDefaultKafkaConnectWithScramShaUser(String namespace, String connectName, String kafkaName, String kafkaUserName, int replicas) {
        return new KafkaConnectBuilder()
            .withNewMetadata()
                .withName(connectName)
                .withNamespace(namespace)
                .addToAnnotations("strimzi.io/use-connector-resources", "true")
            .endMetadata()
            .editOrNewSpec()
                .withVersion(Environment.ST_KAFKA_VERSION)
                .withBootstrapServers(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
                .withGroupId(KafkaConnectResources.componentName(connectName))
                .withConfigStorageTopic(KafkaConnectResources.configMapName(connectName))
                .withOffsetStorageTopic(KafkaConnectResources.configStorageTopicOffsets(connectName))
                .withStatusStorageTopic(KafkaConnectResources.configStorageTopicStatus(connectName))
                .withReplicas(replicas)
                .withNewKafkaClientAuthenticationScramSha512()
                    .withNewPasswordSecret()
                        .withPassword("password")
                        .withSecretName(kafkaUserName)
                    .endPasswordSecret()
                    .withUsername(kafkaUserName)
                .endKafkaClientAuthenticationScramSha512()
                .addToConfig("config.storage.replication.factor", "-1")
                .addToConfig("offset.storage.replication.factor", "-1")
                .addToConfig("status.storage.replication.factor", "-1")
                .withNewInlineLogging()
                    .addToLoggers("rootLogger.level", "DEBUG")
                .endInlineLogging()
            .endSpec();
    }

    public static KafkaConnectBuilder kafkaDefaultConnectWithFilePlugin(String namespace, String connectName, String kafkaName, String kafkaUser, int replicas) {
        return addFilePluginOrImage(namespace, getDefaultKafkaConnectWithScramShaUser(namespace, connectName, kafkaName, kafkaUser, replicas));
    }

    /**
     * Configures a {@link KafkaConnectBuilder} to use either:
     * <ul>
     *   <li>a dynamically built image containing the File plugin, or</li>
     *   <li>a prebuilt image provided via environment configuration.</li>
     * </ul>
     *
     * <p>The behavior depends on the {@code CONNECT_IMAGE_WITH_FILE_PLUGIN} environment variable:
     *
     * <ul>
     *   <li><strong>If empty:</strong>
     *     <ul>
     *       <li>Creates a {@code file-plugin} definition using the JAR artifact URL
     *           from {@code ST_FILE_PLUGIN_URL}.</li>
     *       <li>Generates a unique image tag using a random integer.</li>
     *       <li>Configures the Kafka Connect {@code build} section to:
     *         <ul>
     *           <li>Include the File plugin</li>
     *           <li>Push the built image to the internal registry resolved via
     *               {@code Environment.getImageOutputRegistry(...)}.</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     *   <li><strong>If set:</strong>
     *     <ul>
     *       <li>Skips the build configuration.</li>
     *       <li>Uses the provided image directly in the Kafka Connect specification.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>This method is primarily intended for test environments where the File plugin
     * may either be built on the fly or supplied as a prebuilt container image.
     *
     * @param namespace           the Kubernetes namespace used to resolve the internal image registry
     * @param kafkaConnectBuilder the {@link KafkaConnectBuilder} to configure
     * @return the updated {@link KafkaConnectBuilder} with either build configuration
     *         or a direct image reference applied
     */
    public static KafkaConnectBuilder addFilePluginOrImage(String namespace, KafkaConnectBuilder kafkaConnectBuilder) {
        if (Environment.CONNECT_IMAGE_WITH_FILE_PLUGIN.isEmpty()) {
            Plugin filePlugin = new PluginBuilder()
                .withName("file-plugin")
                .withArtifacts(new JarArtifactBuilder()
                    .withUrl(Environment.ST_FILE_PLUGIN_URL)
                    .build())
                .build();

            // Sonarcube safe image tag with positive uuid
            String tag = String.valueOf(UUID.randomUUID().hashCode() & 0x7fffffff);
            String imageFullPath = Environment.getConnectImageOutputRegistry(namespace, Constants.CONNECT_BUILD_IMAGE_NAME, tag);
            LOGGER.info("Kafka connect build image [{}]", imageFullPath);

            return kafkaConnectBuilder
                .editOrNewSpec()
                    .editOrNewBuild()
                        .withPlugins(filePlugin)
                        .withOutput(dockerOutput(imageFullPath))
                    .endBuild()
                .endSpec();
        } else {
            LOGGER.info("Using {} image from {} env variable", Environment.CONNECT_IMAGE_WITH_FILE_PLUGIN, Environment.CONNECT_IMAGE_WITH_FILE_PLUGIN);
            return kafkaConnectBuilder
                .editOrNewSpec()
                    .withImage(Environment.CONNECT_IMAGE_WITH_FILE_PLUGIN)
                .endSpec();
        }
    }

    /**
     * Creates a {@link DockerOutput} configuration for a Kafka Connect build.
     *
     * <p>This method sets the target image for the build, and optionally attaches a
     * push secret if {@link Environment#CONNECT_BUILD_REGISTRY_SECRET} is defined.
     * The resulting {@link DockerOutput} can be used in the {@code build} section
     * of a {@link KafkaConnectBuilder} to push a custom image to a container registry.
     *
     * @param imageName the full image name including registry, namespace, image name, and tag
     * @return a {@link DockerOutput} object configured with the image and optional push secret
     */
    private static DockerOutput dockerOutput(String imageName) {
        DockerOutputBuilder dockerOutputBuilder = new DockerOutputBuilder().withImage(imageName);
        if (Environment.CONNECT_BUILD_REGISTRY_SECRET != null && !Environment.CONNECT_BUILD_REGISTRY_SECRET.isEmpty()) {
            dockerOutputBuilder.withPushSecret(Environment.CONNECT_BUILD_REGISTRY_SECRET);
        }

        return dockerOutputBuilder.build();
    }


    public static KafkaConnectorBuilder defaultFileSourceConnector(String namespaceName, String connectorName, String connectName, String topicName, int maxTasks) {
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
                .addToConfig("topic", topicName)
            .endSpec();
    }

    public static KafkaConnectorBuilder defaultFileSinkConnector(String namespaceName, String connectorName, String connectName, String topicName, int maxTasks) {
        return new KafkaConnectorBuilder()
            .withNewMetadata()
                .withName(connectorName)
                .withNamespace(namespaceName)
                .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, connectName)
            .endMetadata()
            .editOrNewSpec()
                .withTasksMax(maxTasks)
                .withClassName("org.apache.kafka.connect.file.FileStreamSinkConnector")
                .addToConfig("file", Constants.DEFAULT_SINK_FILE_PATH)
                .addToConfig("topics", topicName)
            .endSpec();
    }

    /**
     * Creates and applies a {@link NetworkPolicy} to allow communication from the Console
     * to a specific Kafka Connect cluster.
     *
     * <p>By default, network policies for Kafka Connect pods may block access. This method
     * creates a policy that allows traffic from the Console deployment to the Kafka Connect
     * pods on the configured {@link Constants#CONNECT_SERVICE_PORT} using TCP.
     *
     * <p>The policy selects pods labeled with the given Kafka Connect cluster name and kind.
     * It is named "{connectName}console-allow" and applied in the provided namespace.
     *
     * @param namespace the Kubernetes namespace where the Kafka Connect cluster and Console are deployed
     * @param consoleInstanceName the name of the Console instance to allow access from
     * @param connectName the name of the Kafka Connect cluster to which access should be allowed
     */
    public static void allowConnectConsoleNetworkPolicy(String namespace, String consoleInstanceName, String connectName) {
        LOGGER.info("Apply NetworkPolicy for connect as it's disabled by default");

        NetworkPolicy networkPolicy = new NetworkPolicyBuilder()
            .withNewMetadata()
                .withName(connectName + "console-allow")
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
