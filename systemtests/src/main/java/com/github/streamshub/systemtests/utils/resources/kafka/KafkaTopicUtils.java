package com.github.streamshub.systemtests.utils.resources.kafka;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.resources.ResourceOperation;
import com.github.streamshub.systemtests.templates.KafkaTopicTemplates;
import com.github.streamshub.systemtests.utils.CommonUtils;
import com.github.streamshub.systemtests.utils.TestStorage;
import com.github.streamshub.systemtests.utils.resources.ResourceManagerUtils;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.streamshub.systemtests.enums.CustomResourceStatus.Ready;
import static com.github.streamshub.systemtests.utils.CommonUtils.waitFor;

public class KafkaTopicUtils {
    private static final Logger LOGGER = LogManager.getLogger(KafkaTopicUtils.class);

    // -------------
    // Client
    // -------------
    public static MixedOperation<KafkaTopic, KafkaTopicList, Resource<KafkaTopic>> kafkaTopicClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(KafkaTopic.class, KafkaTopicList.class);
    }

    // -------------
    // Get
    // -------------
    public static KafkaTopic getTopic(String namespace, String name) {
        return kafkaTopicClient().inNamespace(namespace).withName(name).get();
    }

    public static KafkaTopic getNew(String namespace, String kafkaName, String topicName, int partitions, int replicas, int minIsr) {
        return KafkaTopicTemplates.topic(namespace, kafkaName, topicName, partitions, replicas, minIsr).build();
    }


    public static List<KafkaTopic> getTopicsByCluster(String namespace, String kafkaName) {
        return listTopics(namespace)
            .stream()
            .filter(t -> t.getMetadata().getLabels().get(Labels.STRIMZI_CLUSTER).equals(kafkaName)).toList();
    }

    public static int getTopicsTotalPartitionReplicasOfClusters(String namespace, String kafkaName) {
        int totalPartitionReplicas = 0;

        for (KafkaTopic topic : getTopicsByCluster(namespace, kafkaName)) {
            totalPartitionReplicas += topic.getSpec().getPartitions();
        }

        return totalPartitionReplicas;
    }

    public static String getTopicId(String namespaceName, String topicName) {
        return getTopic(namespaceName, topicName).getStatus().getTopicId();
    }

    // -------------
    // List
    // -------------
    public static List<KafkaTopic> listTopics(String namespace) {
        LOGGER.debug("Listing topics in namespace [{}]", namespace);
        return namespace.equals("--all") ? kafkaTopicClient().inAnyNamespace().list().getItems() : kafkaTopicClient().inNamespace(namespace).list().getItems();
    }

    public static List<KafkaTopic> listTopicsByPrefix(String namespace, String prefix) {
        return listTopics(namespace).stream().filter(kt -> kt.getMetadata().getName().startsWith(prefix)).toList();
    }

    public static List<KafkaTopic> listAllTopics() {
        return listTopics("--all");
    }

    // -------------
    // Create
    // -------------
    public static List<KafkaTopic> createTopicsWithWait(TestStorage ts, String topicNamePrefix, int numberToCreate) {
        return createTopics(ts.getNamespaceName(), ts.getClusterName(), topicNamePrefix, numberToCreate, true, 1, 1, 1);
    }

    public static List<KafkaTopic> createTopicsWithWait(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate) {
        return createTopics(namespace, kafkaName, topicNamePrefix, numberToCreate, true, 1, 1, 1);
    }

    public static List<KafkaTopic> createTopicsWithWait(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate, int partitions, int replicas, int minIsr) {
        return createTopics(namespace, kafkaName, topicNamePrefix, numberToCreate, true, partitions, replicas, minIsr);
    }

    public static List<KafkaTopic> createTopics(String namespace, String kafkaName, String topicNamePrefix, int numberToCreate, boolean waitForTopic, int partitions, int replicas, int minIsr) {
        LOGGER.info("Create {} topics", numberToCreate);
        List<KafkaTopic> topics = new ArrayList<>();

        for (int i = 0; i < numberToCreate; i++) {
            topics.add(KafkaTopicUtils.getNew(namespace, kafkaName, topicNamePrefix + "-" + i, partitions, replicas, minIsr));
        }

        for (KafkaTopic topic : topics) {
            KubeResourceManager.getInstance().createResourceWithWait(topic);
        }

        if (waitForTopic) {
            for (KafkaTopic topic : topics) {
                waitForKafkaTopicReady(namespace, topic.getMetadata().getName());
            }
        }
        return topics;
    }

    public static void createTopic(TestStorage ts, int partitions, int replicas, int minIsr) {
        createTopic(ts.getNamespaceName(), ts.getClusterName(), ts.getTopicName(), partitions, replicas, minIsr);
    }

    public static void createTopic(String namespace, String kafkaName, String topicName) {
        createTopic(namespace, kafkaName, topicName, 1, 1, 1);
    }

    public static void createTopic(String namespace, String kafkaName, String topicName, int partitions, int replicas, int minIsr) {
        KubeResourceManager.getInstance().createResourceWithoutWait(KafkaTopicUtils.getNew(namespace, kafkaName, topicName, partitions, replicas, minIsr));
    }

    // -------------
    // Wait
    // -------------
    public static void waitForTopicListToBeDeleted(String namespace, List<String> topicNamesToBeDeleted) {
        for (String topicName : topicNamesToBeDeleted) {
            waitForTopicToBeDeleted(namespace, topicName);
        }
    }

    public static void waitForTopicToBeDeleted(String namespace, String topicName) {
        waitFor("topic " + topicName + " to be removed from cluster", TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.GLOBAL_TIMEOUT_LONG,
            () -> {
                return listTopics(namespace).stream()
                    .filter(kt -> kt.getMetadata()
                        .getName()
                        .equals(topicName))
                    .toList()
                    .isEmpty();
            },
            () -> LOGGER.error("Topics was not removed {} - TIMEOUT", topicName));
    }

    public static boolean waitForKafkaTopicReady(String namespaceName, String topicName) {
        return waitForKafkaTopicStatus(namespaceName, topicName, Ready);
    }

    public static boolean waitForKafkaTopicStatus(String namespaceName, String topicName, Enum<?> conditionType) {
        return waitForKafkaTopicStatus(namespaceName, topicName, conditionType, ConditionStatus.True);
    }

    public static boolean waitForKafkaTopicStatus(String namespaceName, String topicName, Enum<?> conditionType, ConditionStatus conditionStatus) {
        return ResourceManagerUtils.waitForResourceStatus(kafkaTopicClient(), KafkaTopic.RESOURCE_KIND,
            namespaceName, topicName, conditionType, conditionStatus, ResourceOperation.getTimeoutForResourceReadiness(KafkaTopic.RESOURCE_KIND));
    }

    // -------------
    // Delete
    // -------------
    public static void deleteTopic(String namespace, String name) {
        kafkaTopicClient().inNamespace(namespace).withName(name).delete();
    }

    public static void deleteTopics(List<KafkaTopic> topicsToDelete) {
        LOGGER.info("Delete list of topics");
        if (!topicsToDelete.isEmpty()) {
            for (KafkaTopic topic : topicsToDelete) {
                KubeResourceManager.getInstance().deleteResource(topic);
            }

            waitForTopicListToBeDeleted(topicsToDelete.get(0).getMetadata().getNamespace(),
                topicsToDelete.stream().map(topic -> topic.getMetadata().getName()).toList());
        }
    }

    public static void deleteAllTopicsUsingCli(TestStorage ts) {
        deleteAllTopicsUsingCli(ts.getNamespaceName(), ts.getClusterName());
    }

    public static void deleteAllTopicsUsingCli(String namespaceName, String kafkaName) {
        deleteTopics(listTopics(namespaceName));

        String kafkaPodName = KafkaUtils.getKafkaBrokerPodNameByPrefix(kafkaName, 0);
        String bootstrapAddress = KafkaUtils.getPlainBootstrapAddress(kafkaName);

        List<String> topicNames = listTopicsUsingPodCli(namespaceName, kafkaPodName, bootstrapAddress);
        removeTopicsByNamesUsingPodCli(namespaceName, kafkaPodName, bootstrapAddress, topicNames);
    }

    public static void deleteOffsetTopicsIfPresent(String namespaceName, String kafkaName) {
        LOGGER.debug("Removing Consumer Offset topic if present");
        String kafkaPodName = KafkaUtils.getKafkaBrokerPodNameByPrefix(kafkaName, 0);
        String kafkaBootstrapAddress = KafkaUtils.getPlainBootstrapAddress(kafkaName);
        if (listTopicsUsingPodCli(namespaceName, kafkaPodName, kafkaBootstrapAddress).contains(Constants.CONSUMER_OFFSETS_TOPIC_NAME)) {
            LOGGER.debug("Removing Consumer Offset topic");
            removeConsumerOffsetTopicUsingPodCli(namespaceName, kafkaPodName, kafkaBootstrapAddress);
        }
    }

    public static List<String> removeConsumerOffsetTopicUsingPodCli(String namespaceName, String podName, String bootstrapServer) {
        LOGGER.debug("Removing __consumer_offsets KafkaTopic in Namespace {}", namespaceName);
        CommonUtils.waitFor("consumer offset topic deletion", TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.GLOBAL_TIMEOUT,
            () -> listTopicsUsingPodCli(namespaceName, podName, bootstrapServer).toString().contains(Constants.CONSUMER_OFFSETS_TOPIC_NAME),
            () -> {
                LOGGER.error("Consumer offset topic not deleted");
                listTopicsUsingPodCli(namespaceName, podName, bootstrapServer);
            });

        return removeTopicUsingPodCli(namespaceName, podName, bootstrapServer, Constants.CONSUMER_OFFSETS_TOPIC_NAME);
    }

    // -------------
    // CMD
    // -------------
    public static List<String> listTopicsUsingPodCli(String namespaceName, String kafkaName) {
        String kafkaPodName = KafkaUtils.getKafkaBrokerPodNameByPrefix(kafkaName, 0);
        String kafkaBootstrapAddress = KafkaUtils.getPlainBootstrapAddress(kafkaName);
        return listTopicsUsingPodCli(namespaceName, kafkaPodName, kafkaBootstrapAddress);
    }

    public static List<String> listTopicsUsingPodCli(String namespaceName, String podName, String bootstrapServer) {
        LOGGER.debug("List topics using CLI from Pod: {}/{}", namespaceName, podName);
        List<String> topics = Arrays.stream(KubeResourceManager.getKubeCmdClient().inNamespace(namespaceName).execInPod(podName, "/bin/bash", "-c",
            "bin/kafka-topics.sh --list --bootstrap-server " + bootstrapServer + " | sed 's/^/=-=/'").out().split("=-="))
            .filter(i -> !i.isEmpty()).toList();
        LOGGER.debug("Found following topics using CLI: [{}]", String.join("; ", topics));
        return topics;
    }

    public static int getTopicsCountUsingPodCli(String namespaceName, String kafkaName) {
        String kafkaPodName = KafkaUtils.getKafkaBrokerPodNameByPrefix(kafkaName, 0);
        String kafkaBootstrapAddress = KafkaUtils.getPlainBootstrapAddress(kafkaName);
        return listTopicsUsingPodCli(namespaceName, kafkaPodName, kafkaBootstrapAddress).size();
    }

    public static void removeTopicsUsingPodCli(String namespaceName, String podName, String bootstrapServer, List<KafkaTopic> topics) {
        for (KafkaTopic topic: topics) {
            removeTopicUsingPodCli(namespaceName, podName, bootstrapServer, topic.getMetadata().getName());
        }
    }

    public static void removeTopicsByNamesUsingPodCli(String namespaceName, String kafkaName, List<String> topicNames) {
        String kafkaPodName = KafkaUtils.getKafkaBrokerPodNameByPrefix(kafkaName, 0);
        String kafkaBootstrapAddress = KafkaUtils.getPlainBootstrapAddress(kafkaName);
        removeTopicsByNamesUsingPodCli(namespaceName, kafkaPodName, kafkaBootstrapAddress, topicNames);
    }

    public static void removeTopicsByNamesUsingPodCli(String namespaceName, String podName, String bootstrapServer, List<String> topicNames) {
        for (String topicName: topicNames) {
            removeTopicUsingPodCli(namespaceName, podName, bootstrapServer, topicName);
        }
        if (!listTopicsUsingPodCli(namespaceName, podName, bootstrapServer).isEmpty()) {
            removeTopicsByNamesUsingPodCli(namespaceName, podName, bootstrapServer, listTopicsUsingPodCli(namespaceName, podName, bootstrapServer));
        }
    }

    public static List<String> removeTopicUsingPodCli(String namespaceName, String podName, String bootstrapServer, String topicName) {
        LOGGER.debug("Removing kafkaTopic {} by CLI using Pod {}/{}", topicName, namespaceName, podName);
        return Arrays.asList(KubeResourceManager.getKubeCmdClient().inNamespace(namespaceName).execInPod(podName, "/bin/bash", "-c",
            "bin/kafka-topics.sh --bootstrap-server " + bootstrapServer + " --delete --topic " + topicName + " | sed 's/^/=-=/'").out().split("=-="));
    }

    public static String getTopicPartitionLeaderUsingPodCli(String namespaceName, String kafkaName, String topicName) {
        String kafkaPodName = KafkaUtils.getKafkaBrokerPodNameByPrefix(kafkaName, 0);
        String kafkaBootstrapAddress = KafkaUtils.getPlainBootstrapAddress(kafkaName);
        return getTopicPartitionLeaderUsingPodCli(namespaceName, kafkaPodName, kafkaBootstrapAddress, topicName);
    }

    public static String getTopicPartitionLeaderUsingPodCli(String namespaceName, String podName, String bootstrapServer, String topicName) {
        LOGGER.debug("Get kafkaTopic partitions {} by CLI using Pod {}/{}", topicName, namespaceName, podName);
        return Arrays.stream(KubeResourceManager.getKubeCmdClient().inNamespace(namespaceName).execInPod(podName, "/bin/bash", "-c",
            "bin/kafka-topics.sh --bootstrap-server " + bootstrapServer + " --describe --topic " + topicName).out()
                .split("\\t")).filter(s -> s.contains("Leader")).toList().get(0).replace("Leader: ", "");
    }

    String removeFinalizers(String namespace, String topicName) {
        return KubeResourceManager.getKubeCmdClient().inNamespace(namespace).exec("get kafkatopic/" + topicName + " -o=json | sed 's/\"finalizers\":\\[[^]]*\\]/\"finalizers\":[]/g' | kubectl replace -f -").out();
    }
}
