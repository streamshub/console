package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.utils.PwUtils;
import com.github.streamshub.systemtests.utils.resources.KafkaNodePoolUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

import java.util.UUID;


public class TestStorage {
    private String namespaceName;
    private int brokerReplicas;
    // Kafka resource names
    private String clusterName;
    private String brokerPoolName;
    private String controllerPoolName;
    private String topicPrefixName;
    private String kafkaUsername;
    private int messageCount;
    private String producerName;
    private String consumerName;
    // Selectors
    private LabelSelector brokerPoolSelector;
    private LabelSelector controllerPoolSelector;
    // Playwright
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    public TestStorage(String namespaceName) {
        this(namespaceName, Constants.MESSAGE_COUNT);
    }

    public TestStorage(String namespaceName, int messageCount) {

        this.namespaceName = namespaceName;
        this.brokerReplicas = Constants.REGULAR_BROKER_REPLICAS;
        // Kafka resource names
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
        this.clusterName = Constants.KAFKA_CLUSTER_PREFIX + "-" + randomSuffix;
        this.brokerPoolName = Constants.BROKER_ROLE_PREFIX + "-" + randomSuffix;
        this.controllerPoolName = Constants.CONTROLLER_ROLE_PREFIX + "-" + randomSuffix;
        this.topicPrefixName = Constants.KAFKA_TOPIC_PREFIX + "-" + randomSuffix;
        this.kafkaUsername = Constants.KAFKA_USER_PREFIX + "-" + randomSuffix;
        this.producerName = topicPrefixName + "-" + Constants.PRODUCER;
        this.consumerName = topicPrefixName + "-" + Constants.CONSUMER;
        // Selectors
        this.brokerPoolSelector = KafkaNodePoolUtils.getKnpLabelSelector(clusterName, this.brokerPoolName, ProcessRoles.BROKER);
        this.controllerPoolSelector = KafkaNodePoolUtils.getKnpLabelSelector(clusterName, this.controllerPoolName, ProcessRoles.CONTROLLER);
        this.messageCount = messageCount;
        // Playwright
        this.playwright = Playwright.create();
        this.browser = PwUtils.createAndReturnBrowser(playwright);
        this.context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        this.page = context.newPage();
    }
}

