package com.github.streamshub.systemtests.utils.resources;

import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.exceptions.UnsupportedKafkaRoleException;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

import java.util.HashMap;
import java.util.Map;

public class KafkaNodePoolUtils {
    private KafkaNodePoolUtils() {}
    // Selector
    public static LabelSelector getKnpLabelSelector(String clusterName, String poolName, ProcessRoles processRole) {
        Map<String, String> matchLabels = new HashMap<>();

        matchLabels.put(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName);
        matchLabels.put(ResourceLabels.STRIMZI_KIND_LABEL, ResourceKinds.KAFKA);
        matchLabels.put(Labels.STRIMZI_POOL_NAME_LABEL, poolName);

        switch (processRole) {
            case BROKER -> matchLabels.put(Labels.STRIMZI_BROKER_ROLE_LABEL, "true");
            case CONTROLLER -> matchLabels.put(Labels.STRIMZI_CONTROLLER_ROLE_LABEL, "true");
            default -> throw new UnsupportedKafkaRoleException("KafkaNodePool without specified role is unsupported");
        }

        return new LabelSelectorBuilder().withMatchLabels(matchLabels).build();
    }
}
