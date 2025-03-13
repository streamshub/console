package com.github.streamshub.systemtests.utils.resources.kafka;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.enums.CustomResourceStatus;
import com.github.streamshub.systemtests.utils.CommonUtils;
import com.github.streamshub.systemtests.utils.TestStorage;
import com.github.streamshub.systemtests.utils.resources.ResourceManagerUtils;
import com.github.streamshub.systemtests.utils.resources.kubernetes.PodUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaList;

import java.util.List;
import java.util.Locale;

import static com.github.streamshub.systemtests.utils.resources.kafka.KafkaNodePoolUtils.brokerPodName;

public class KafkaUtils {
    // -------------
    // Client
    // -------------
    public static MixedOperation<Kafka, KafkaList, Resource<Kafka>> kafkaClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(Kafka.class, KafkaList.class);
    }

    // -------------
    // Get
    // -------------
    public static Kafka getKafka(TestStorage ts) {
        return getKafka(ts.getNamespaceName(), ts.getClusterName());
    }

    public static Kafka getKafka(String namespace, String name) {
        return kafkaClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static KafkaList listKafkas(String namespace) {
        return kafkaClient().inNamespace(namespace).list();
    }
    public static List<Pod> listPodsByPrefixInName(String namespace, String kafkaClusterName) {
        return PodUtils.listPodsByPrefixInName(namespace, kafkaClusterName + "-" + ResourceKinds.KAFKA.toLowerCase(Locale.ENGLISH));
    }

    // -------------
    // Wait
    // -------------
    public static boolean waitForKafkaStatus(String namespaceName, String clusterName, Enum<?> state) {
        Kafka kafka = getKafka(namespaceName, clusterName);
        return ResourceManagerUtils.waitForResourceStatus(kafkaClient(), kafka, state);
    }

    public static boolean waitForKafkaReady(TestStorage ts) {
        PodUtils.waitForPodsReady(ts.getNamespaceName(), ts.getControllerPoolSelector(), KafkaNodePoolUtils.getKafkaBrokerReplicas(ts), true);
        PodUtils.waitForPodsReady(ts.getNamespaceName(), ts.getBrokerPoolSelector(), KafkaNodePoolUtils.getKafkaControllerReplicas(ts), true);
        return waitForKafkaReady(ts.getNamespaceName(), ts.getClusterName());
    }

    public static boolean waitForKafkaReady(String namespaceName, String clusterName) {
        return waitForKafkaStatus(namespaceName, clusterName, CustomResourceStatus.Ready);
    }

    // -------------
    // Delete
    // -------------
    public static void deleteKafka(String namespace, String kafkaName) {
        kafkaClient().inNamespace(namespace).withName(kafkaName).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    // -------------
    // Kafka names
    // -------------
    public static String kafkaClusterName(String namespaceName) {
        return Constants.KAFKA_CLUSTER_PREFIX + "-" + CommonUtils.hashStub(namespaceName);
    }

    public static String getKafkaMetricsConfigMapName(String kafkaName) {
        return kafkaName + "-metrics";
    }

    public static String getPlainBootstrapAddress(String clusterName) {
        return getBootstrapServiceName(clusterName) + ":9092";
    }

    public static String getBootstrapServiceName(String clusterName) {
        return clusterName + "-kafka-bootstrap";
    }

    public static String getKafkaBrokerPodNameByPrefix(String kafkaName, int podIndex) {
        return brokerPodName(kafkaName) + "-" + podIndex;
    }
}
