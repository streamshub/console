package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBuilder;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class KafkaUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaUtils.class);

    private KafkaUtils() {}

    public static String getPlainBootstrapAddress(String clusterName) {
        return getBootstrapServiceName(clusterName) + ":9092";
    }

    public static String getPlainScramShaBootstrapAddress(String clusterName) {
        return getBootstrapServiceName(clusterName) + ":9095";
    }

    public static String getBootstrapServiceName(String clusterName) {
        return clusterName + "-kafka-bootstrap";
    }

    /**
     * Adds an annotation to a Kafka resource in the specified namespace.
     *
     * <p>If the annotation key does not exist or its value is different from the provided one,
     * this method updates the Kafka custom resource metadata with the new annotation.</p>
     * <p>If {@code wait} is {@code true}, it waits until the annotation with the expected value appears on the Kafka resource.</p>
     *
     * @param namespace the Kubernetes namespace where the Kafka resource is located
     * @param kafkaName the name of the Kafka custom resource
     * @param annoKey the annotation key to add or update
     * @param annoVal the annotation value to set
     * @param wait whether to wait for the annotation to be applied
     */
    public static void addAnnotation(String namespace, String kafkaName, String annoKey, String annoVal, boolean wait) {
        LOGGER.info("Adding annotation: {}:{} to Kafka: {}/{}", annoKey, annoVal, namespace, kafkaName);
        Kafka oldKafka = ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName);
        Map<String, String> anno = oldKafka.getMetadata().getAnnotations();
        if (!java.util.Objects.equals(anno.get(annoKey), annoVal)) {
            anno.put(annoKey, annoVal);
            KubeResourceManager.get().createOrUpdateResourceWithWait(
                new KafkaBuilder(oldKafka)
                .editMetadata()
                    .withAnnotations(anno)
                .endMetadata()
                .build()
            );
        }

        if (wait) {
            WaitUtils.waitForKafkaHasAnnotationWithValue(namespace, kafkaName, annoKey, annoVal);
        }
    }

    /**
     * Removes a specific annotation from a Kafka resource in the given namespace.
     *
     * <p>If the Kafka resource contains the specified annotation key, it is removed from the metadata.</p>
     * <p>If {@code wait} is {@code true}, the method waits until the annotation is no longer present on the resource.</p>
     *
     * @param namespace the Kubernetes namespace where the Kafka resource is located
     * @param kafkaName the name of the Kafka custom resource
     * @param annoKey the annotation key to remove
     * @param wait whether to wait for the annotation to be removed
     */
    public static void removeAnnotation(String namespace, String kafkaName, String annoKey, boolean wait) {
        LOGGER.info("Removing annotation: {} from Kafka: {}/{}", annoKey, namespace, kafkaName);
        Kafka oldKafka = ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName);
        Map<String, String> anno = oldKafka.getMetadata().getAnnotations();
        if (anno.containsKey(annoKey)) {
            anno.remove(annoKey);
            KubeResourceManager.get().createOrUpdateResourceWithWait(
                new KafkaBuilder(oldKafka)
                .editMetadata()
                    .withAnnotations(anno)
                .endMetadata()
                .build()
            );
        }

        if (wait) {
            WaitUtils.waitForKafkaHasNoAnnotationWithKey(namespace, kafkaName, annoKey);
        }
    }

    public static void scaleBrokerReplicas(String namespace, String kafkaName, int scaledBrokersCount) {
        LOGGER.info("Scale Kafka broker replicas to {}", scaledBrokersCount);

        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaNodePoolBuilder(ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName)))
                .editSpec()
                    .withReplicas(scaledBrokersCount)
                .endSpec()
                .build());

        // Wait for change of replicas, then get the nodeIds
        WaitUtils.waitForKafkaBrokerNodePoolReplicasInSpec(namespace, kafkaName, scaledBrokersCount);

        List<String> nodeIds = List.of(ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName))
            .getStatus()
            .getNodeIds()
            .toString()
            .replace("[", "")
            .replace(" ", "")
            .replace("]", "")
            .split(","));

        // Create new listener config spec
        String hashedNamespace = Utils.hashStub(namespace);
        GenericKafkaListenerConfigurationBuilder configurationBuilder = new GenericKafkaListenerConfigurationBuilder();

        configurationBuilder = configurationBuilder
            .withNewBootstrap()
                .withHost(String.join(".", "bootstrap", hashedNamespace, kafkaName, ClusterUtils.getClusterDomain()))
            .endBootstrap();

        // Add each broker host with broker ID into the config
        for (String nodeId : nodeIds) {
            configurationBuilder = configurationBuilder.addToBrokers(
                new GenericKafkaListenerConfigurationBrokerBuilder()
                    .withBroker(Integer.parseInt(nodeId))
                    .withHost(String.join(".", "broker-" + nodeId, hashedNamespace, kafkaName, ClusterUtils.getClusterDomain()))
                    .build());
        }

        // Finally apply the new listener config to Kafka to avoid ingress issues caused by missing listener broker ids
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaBuilder(ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName))
                .editSpec()
                    .editKafka()
                        .editMatchingListener(l -> l.getName().equals(Constants.SECURE_LISTENER_NAME))
                            .withConfiguration(configurationBuilder.build())
                        .endListener()
                    .endKafka()
                .endSpec()
                .build());

        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), scaledBrokersCount, true);
        LOGGER.info("Kafka broker replicas successfully scaled to {}", scaledBrokersCount);
    }
}
