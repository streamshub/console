package com.github.streamshub.systemtests.constants;

import com.github.streamshub.systemtests.exceptions.UnsupportedKafkaRoleException;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
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
    public static final String APP = "app";


    // ------------
    // Strimzi
    // ------------
    public static final String STRIMZI_POOL_NAME_LABEL = ResourceLabels.STRIMZI_DOMAIN + "pool-name";
    public static final String STRIMZI_BROKER_ROLE_LABEL = ResourceLabels.STRIMZI_DOMAIN + "broker-role";
    public static final String STRIMZI_CONTROLLER_ROLE_LABEL = ResourceLabels.STRIMZI_DOMAIN + "controller-role";

    // ------------
    // Kafka
    // ------------
    public static final String KAFKA_CLIENTS_LABEL_KEY = "user-test-app";
    public static final String KAFKA_CLIENTS_LABEL_VALUE = "kafka-clients";

    public static Map<String, String> getClientsLabels(String clientName) {
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put(APP, clientName);
        matchLabels.put(KAFKA_CLIENTS_LABEL_KEY, KAFKA_CLIENTS_LABEL_VALUE);
        return matchLabels;
    }

    /**
     * Creates a {@link LabelSelector} for a KafkaNodePool based on cluster name, pool name, and process role.
     *
     * @param clusterName the name of the Kafka cluster
     * @param poolName the KafkaNodePool name (e.g., broker or controller pool)
     * @param processRole the {@link ProcessRoles} enum indicating the role (BROKER or CONTROLLER)
     * @return a {@link LabelSelector} matching the specified labels
     * @throws UnsupportedKafkaRoleException if the process role is not BROKER or CONTROLLER
     */
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

    /**
     * Returns a {@link LabelSelector} for the KafkaNodePool brokers of the specified cluster.
     *
     * @param clusterName the name of the Kafka cluster
     * @return a {@link LabelSelector} selecting broker pods in the cluster
     */
    public static LabelSelector getKnpBrokerLabelSelector(String clusterName) {
        return getKnpLabelSelector(clusterName, KafkaNamingUtils.brokerPoolName(clusterName), ProcessRoles.BROKER);
    }

    /**
     * Returns a {@link LabelSelector} for the KafkaNodePool controllers of the specified cluster.
     *
     * @param clusterName the name of the Kafka cluster
     * @return a {@link LabelSelector} selecting controller pods in the cluster
     */
    public static LabelSelector getKnpControllerLabelSelector(String clusterName) {
        return getKnpLabelSelector(clusterName, KafkaNamingUtils.controllerPoolName(clusterName), ProcessRoles.CONTROLLER);
    }

    /**
     * Returns a {@link LabelSelector} matching all Kafka pods belonging to the specified cluster.
     *
     * @param clusterName the name of the Kafka cluster
     * @return a {@link LabelSelector} selecting all Kafka pods of the cluster
     */
    public static LabelSelector getKafkaPodLabelSelector(String clusterName) {
        return new LabelSelectorBuilder().withMatchLabels(Map.of(
                ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName,
                ResourceLabels.STRIMZI_COMPONENT_TYPE_LABEL, Kafka.RESOURCE_KIND.toLowerCase(Locale.ENGLISH)
            ))
            .build();
    }

    public static LabelSelector getNginxPodLabelSelector() {
        return new LabelSelectorBuilder()
            .addToMatchLabels("app.kubernetes.io/component", "controller")
            .addToMatchLabels("app.kubernetes.io/instance", "ingress-nginx")
            .build();
    }
}
