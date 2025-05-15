package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.constants.Labels;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.Map;
import java.util.stream.Collectors;

public class KafkaUtils {

    private KafkaUtils() {}

    public static Map<String, String> createKafkaPodsSnapshots(String namespace, String kafkaName) {
        return ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespace, Labels.getKafkaPodLabelSelector(kafkaName))
            .stream().collect(Collectors.toMap(pod -> pod.getMetadata().getName(), pod -> pod.getMetadata().getUid()));
    }
}
