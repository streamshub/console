package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import org.apache.logging.log4j.Logger;

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
}
