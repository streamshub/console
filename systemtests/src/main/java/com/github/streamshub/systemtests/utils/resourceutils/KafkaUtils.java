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
