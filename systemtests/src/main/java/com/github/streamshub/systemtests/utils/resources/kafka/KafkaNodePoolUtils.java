package com.github.streamshub.systemtests.utils.resources.kafka;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.utils.CommonUtils;
import com.github.streamshub.systemtests.utils.TestStorage;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolList;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaNodePoolUtils {
    // -------------
    // Client
    // -------------
    public static MixedOperation<KafkaNodePool, KafkaNodePoolList, Resource<KafkaNodePool>> kafkaNodePoolClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(KafkaNodePool.class, KafkaNodePoolList.class);
    }

    // -------
    // Get
    // -------
    public static KafkaNodePool getKnpBroker(TestStorage ts) {
        return getKnpBroker(ts.getNamespaceName(), ts.getClusterName());
    }

    public static KafkaNodePool getKnpBroker(String namespace, String kafkaName) {
        return getKafkaNodePool(namespace, brokerPoolName(kafkaName));
    }

    public static KafkaNodePool getKafkaNodePool(String namespace, String name) {
        return kafkaNodePoolClient().inNamespace(namespace).withName(name).get();
    }

    public static int getKafkaBrokerReplicas(TestStorage ts) {
        return getKafkaBrokerReplicas(ts.getNamespaceName(), ts.getClusterName());
    }

    public static int getKafkaBrokerReplicas(String namespace, String kafkaClusterName) {
        return getKafkaNodePool(namespace, brokerPoolName(kafkaClusterName)).getSpec().getReplicas();
    }

    public static int getKafkaControllerReplicas(TestStorage ts) {
        return getKafkaControllerReplicas(ts.getNamespaceName(), ts.getClusterName());
    }

    public static int getKafkaControllerReplicas(String namespace, String kafkaClusterName) {
        return getKafkaNodePool(namespace, brokerPoolName(kafkaClusterName)).getSpec().getReplicas();
    }

    // -------
    // List
    // -------
    public static List<KafkaNodePool> listKafkaNodePools(String namespace) {
        return kafkaNodePoolClient().inNamespace(namespace).list().getItems();
    }

    // -------
    // Selector
    // -------
    public static LabelSelector getLabelSelector(String clusterName, String poolName, ProcessRoles processRole) {
        Map<String, String> matchLabels = new HashMap<>();

        matchLabels.put(Labels.STRIMZI_CLUSTER, clusterName);
        matchLabels.put(Labels.STRIMZI_KIND, ResourceKinds.KAFKA);
        matchLabels.put(Labels.STRIMZI_POOL_NAME, poolName);

        switch (processRole) {
            case BROKER -> matchLabels.put(Labels.STRIMZI_BROKER_ROLE, "true");
            case CONTROLLER -> matchLabels.put(Labels.STRIMZI_CONTROLLER_ROLE, "true");
            default -> throw new RuntimeException("No role for KafkaNodePool specified");
        }

        return new LabelSelectorBuilder()
            .withMatchLabels(matchLabels)
            .build();
    }

    // -------------
    // Kafka names
    // -------------
    public static String brokerPoolName(String kafkaName) {
        return Constants.BROKER_ROLE_PREFIX + "-" + CommonUtils.hashStub(kafkaName);
    }

    public static String brokerPodName(String kafkaName) {
        return kafkaName + "-" + Constants.BROKER_ROLE_PREFIX + "-" + CommonUtils.hashStub(kafkaName);
    }

    public static String controllerPoolName(String kafkaName) {
        return Constants.CONTROLLER_ROLE_PREFIX + "-" + CommonUtils.hashStub(kafkaName);
    }
}
