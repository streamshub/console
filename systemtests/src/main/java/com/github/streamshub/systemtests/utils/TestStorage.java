package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaNodePoolUtils;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaUserUtils;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaUtils;
import com.microsoft.playwright.Playwright;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Random;

import static com.github.streamshub.systemtests.utils.CommonUtils.hashStub;

public class TestStorage {

    private static final String EO_DEP_SUFFIX = "entity-operator";
    private static final Random RANDOM = new Random();


    private String testName;
    private String namespaceName;
    private String clusterName;
    private int brokerReplicas;
    private String brokerPoolName;
    private String controllerPoolName;
    private String topicPrefixName;
    private int messageCount;

    private String producerName;
    private String consumerName;
    private String eoDeploymentName;
    private String kafkaUsername;

    private LabelSelector brokerPoolSelector;
    private LabelSelector controllerPoolSelector;

    private final Playwright playwright;

    public TestStorage(ExtensionContext extensionContext, String namespaceName) {
        this(extensionContext, namespaceName, Constants.MESSAGE_COUNT);
    }

    public TestStorage(ExtensionContext extensionContext, String namespaceName, int messageCount) {
        this.testName = extensionContext.getTestMethod().get().getName();

        this.namespaceName = namespaceName;

        this.brokerReplicas = Constants.REGULAR_KAFKA_REPLICAS;

        this.clusterName = KafkaUtils.kafkaClusterName(namespaceName);

        this.brokerPoolName = KafkaNodePoolUtils.brokerPoolName(clusterName);
        this.controllerPoolName = KafkaNodePoolUtils.controllerPoolName(clusterName);

        this.kafkaUsername = KafkaUserUtils.kafkaUserName(clusterName);

        this.topicPrefixName = Constants.KAFKA_TOPIC_PREFIX + "-" + hashStub(clusterName);

        this.producerName = topicPrefixName + "-" + Constants.PRODUCER;
        this.consumerName = topicPrefixName + "-" + Constants.CONSUMER;

        this.eoDeploymentName = clusterName + "-" + EO_DEP_SUFFIX;

        this.brokerPoolSelector = KafkaNodePoolUtils.getLabelSelector(clusterName, this.brokerPoolName, ProcessRoles.BROKER);
        this.controllerPoolSelector = KafkaNodePoolUtils.getLabelSelector(clusterName, this.controllerPoolName, ProcessRoles.CONTROLLER);

        this.messageCount = messageCount;

        this.playwright = Playwright.create();
    }

    public String getTestName() {
        return testName;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getBrokerPoolName() {
        return brokerPoolName;
    }

    public String getControllerPoolName() {
        return controllerPoolName;
    }

    public String getTopicName() {
        return topicPrefixName;
    }

    public String getTopicName(String suffix) {
        return topicPrefixName + "-" + suffix;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String getProducerName() {
        return producerName;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public String getEoDeploymentName() {
        return eoDeploymentName;
    }

    public String getKafkaUsername() {
        return kafkaUsername;
    }

    public LabelSelector getBrokerPoolSelector() {
        return brokerPoolSelector;
    }

    public LabelSelector getControllerPoolSelector() {
        return controllerPoolSelector;
    }

    public int getBrokerReplicas() {
        return brokerReplicas;
    }

    public Playwright getPlaywright() {
        return playwright;
    }

    public String getTopicPrefixName() {
        return topicPrefixName;
    }
}
