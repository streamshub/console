package com.github.streamshub.systemtests.resources;

import com.github.streamshub.systemtests.constants.ResourceKinds;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2;
import io.strimzi.api.kafka.model.podset.StrimziPodSet;

import java.time.Duration;

/**
 * Utility class for defining timeouts for various resource operations.
 */
public class ResourceOperation {
    public static long getTimeoutForResourceReadiness() {
        return getTimeoutForResourceReadiness("default");
    }

    public static long getTimeoutForResourceReadiness(String kind) {
        long timeout;

        switch (kind) {
            case Kafka.RESOURCE_KIND:
                timeout = Duration.ofMinutes(14).toMillis();
                break;
            case KafkaConnect.RESOURCE_KIND:
            case KafkaMirrorMaker2.RESOURCE_KIND:
            case ResourceKinds.DEPLOYMENT_CONFIG:
                timeout = Duration.ofMinutes(10).toMillis();
                break;
            case StrimziPodSet.RESOURCE_KIND:
            case ResourceKinds.DEPLOYMENT:
                timeout = Duration.ofMinutes(8).toMillis();
                break;
            default:
                timeout = Duration.ofMinutes(3).toMillis();
        }

        return timeout;
    }

    public static long timeoutForPodsOperation(int numberOfPods) {
        return Duration.ofMinutes(5).toMillis() * Math.max(1, numberOfPods);
    }

    public static long getTimeoutForResourceDeletion() {
        return getTimeoutForResourceDeletion("default");
    }

    public static long getTimeoutForResourceDeletion(String kind) {
        return switch (kind) {
            case Kafka.RESOURCE_KIND, ResourceKinds.POD -> Duration.ofMinutes(5).toMillis();
            default -> Duration.ofMinutes(3).toMillis();
        };
    }
}
