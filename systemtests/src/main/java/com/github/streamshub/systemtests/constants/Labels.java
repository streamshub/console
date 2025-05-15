package com.github.streamshub.systemtests.constants;

import com.github.streamshub.systemtests.exceptions.UnsupportedKafkaRoleException;
import com.github.streamshub.systemtests.utils.KafkaNamingUtils;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Labels {
    private Labels() {}

    // ------------
    // General
    // ------------
    public static final String COLLECT_ST_LOGS = "streamshub-st";

    // ------------
    // Strimzi
    // ------------
    public static final String STRIMZI_POOL_NAME_LABEL = ResourceLabels.STRIMZI_DOMAIN + "pool-name";
    public static final String STRIMZI_BROKER_ROLE_LABEL = ResourceLabels.STRIMZI_DOMAIN + "broker-role";
    public static final String STRIMZI_CONTROLLER_ROLE_LABEL = ResourceLabels.STRIMZI_DOMAIN + "controller-role";

    // ------------
    // Label selectors
    // ------------
    private static LabelSelector getKnpLabelSelector(String clusterName, String poolName, ProcessRoles processRole) {
        Map<String, String> matchLabels = new HashMap<>();

        matchLabels.put(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName);
        matchLabels.put(ResourceLabels.STRIMZI_COMPONENT_TYPE_LABEL, Kafka.RESOURCE_KIND.toLowerCase(Locale.ENGLISH));
        matchLabels.put(Labels.STRIMZI_POOL_NAME_LABEL, poolName);

        switch (processRole) {
            case BROKER -> matchLabels.put(Labels.STRIMZI_BROKER_ROLE_LABEL, "true");
            case CONTROLLER -> matchLabels.put(Labels.STRIMZI_CONTROLLER_ROLE_LABEL, "true");
            default -> throw new UnsupportedKafkaRoleException("KafkaNodePool without specified Broker or Controller role is unsupported");
        }

        return new LabelSelectorBuilder().withMatchLabels(matchLabels).build();
    }

    public static LabelSelector getKnpBrokerLabelSelector(String clusterName) {
        return getKnpLabelSelector(clusterName, KafkaNamingUtils.brokerPoolName(clusterName), ProcessRoles.BROKER);
    }
    public static LabelSelector getKnpControllerLabelSelector(String clusterName) {
        return getKnpLabelSelector(clusterName, KafkaNamingUtils.controllerPoolName(clusterName), ProcessRoles.CONTROLLER);
    }

}
