package com.github.streamshub.systemtests.auth;

import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.MessageStore;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.KafkaDashboardPageSelectors;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.PodUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.REGRESSION)
public class KafkaCredentialsST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaCredentialsST.class);
    private TestCaseConfig tcc;
    private static final String ALL_TOPIC_TYPES_BUCKET = "AllTopicTypes";

    private static final int REPLICATED_TOPICS_COUNT = 5;
    private static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 4;
    private static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    //
    private static final int UNDER_REPLICATED_TOPICS_COUNT = 3;
    private static final int UNAVAILABLE_TOPICS_COUNT = 2;
    private static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;

    /**
     * Verifies that a user authenticated with Kafka credentials can successfully
     * access the console UI and interact with Kafka cluster resources.
     *
     * <p>The test validates the following functionality:</p>
     * <ul>
     *   <li>Access to the cluster Overview page and correct display of available Kafka clusters.</li>
     *   <li>Correct broker and controller replica counts in the cluster overview.</li>
     *   <li>Proper rendering of the Nodes page, including:
     *       <ul>
     *           <li>Total node count</li>
     *           <li>Working and warning node indicators</li>
     *           <li>Broker and controller role distribution</li>
     *       </ul>
     *   </li>
     *   <li>Correct display of topic statistics on both Overview and Topics pages.</li>
     *   <li>Display of the authenticated Kafka username in the navigation bar.</li>
     *   <li>Pause and resume of Kafka reconciliation via the UI, including:
     *       <ul>
     *           <li>Validation of the confirmation modal content</li>
     *           <li>Verification of reconciliation paused notification</li>
     *           <li>Verification of the underlying Kafka resource annotation change</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p>This test ensures that users authenticated with Kafka credentials have
     * proper read access to cluster information and that UI-triggered actions
     * (pause/resume reconciliation) correctly affect the Kafka custom resource
     * and are reflected both in the UI and at the Kubernetes level.</p>
     */
    @Test
    @TestBucket(ALL_TOPIC_TYPES_BUCKET)
    void testUserLoginKafkaCredentials() {
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        // Verify basic UI elements
        LOGGER.info("Check navbar with list of available kafkas");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Check default kafka broker replicas count");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS,
            PodUtils.getTimeoutForPodOperations(Constants.REGULAR_BROKER_REPLICAS),true);

        LOGGER.debug("Verify default Kafka node count on Nodes page");
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        // Header
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_TOTAL_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT, "0", true);
        // Page infobox
        PwUtils.waitForLocatorCount(tcc, Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, NodesPageSelectors.NPS_TABLE_BODY, true);
        // total nodes
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(1).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with controller role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(2).build(), Integer.toString(Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with broker role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(3).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS), true);
        // Node table
        assertEquals(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, tcc.page().locator(NodesPageSelectors.NPS_TABLE_BODY).all().size());

        LOGGER.info("Check navbar data");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, tcc.kafkaUserName(), true);

        LOGGER.info("Verify topic display");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        // Test pausing kafka reconciliation to confirm UI communicates with kafka
        LOGGER.info("Pause Kafka reconciliation using UI");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        LOGGER.debug("Open pop-up modal for pause reconciliation");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, MessageStore.pauseReconciliationButton(), false);
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        LOGGER.debug("Check pop-up modal for pause reconciliation");
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.PAGES_POPUP_MODAL);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_HEADER, MessageStore.pauseReconciliation(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_BODY, MessageStore.pauseReconciliationText(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, MessageStore.reconciliationCancel(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, MessageStore.reconciliationConfirm(), false);

        LOGGER.debug("Confirm pause reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        // Check aftermath
        LOGGER.info("Verify UI state after pausing Kafka reconciliation");
        PwUtils.waitForLocatorVisible(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION, MessageStore.reconciliationPausedWarning(), false);

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set to true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        LOGGER.info("Resume Kafka reconciliation using UI");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, MessageStore.resumeReconciliation(), true);
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, MessageStore.reconciliationCancel(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, MessageStore.reconciliationConfirm(), false);

        LOGGER.debug("Confirm resume reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        // Reconciliation is resumed and button should display Pause
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, MessageStore.pauseReconciliationButton(), true);

        // Check annotation
        LOGGER.debug("Verify Kafka has pause reconciliation annotation set back to false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");

        PwUtils.logoutUser(tcc, tcc.kafkaUserName(), false);
        LOGGER.info("Stop");
    }

    /**
     * Prepares a set of Kafka topics with various states and replication configurations
     * for UI system testing purposes.
     *
     * <p>This setup method is executed for the {@code ALL_TOPIC_TYPES_BUCKET} and
     * initializes the Kafka cluster with a predefined mix of topic types to validate
     * Streamshub Console topic visualization and status reporting.</p>
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *     <li>Verifies that the initial UI state contains zero topics on both the
     *     Overview and Topics pages.</li>
     *     <li>Creates fully replicated (managed) topics.</li>
     *     <li>Produces a higher number of messages to the last replicated topic
     *     to increase storage usage and simulate realistic load.</li>
     *     <li>Creates unmanaged replicated topics.</li>
     *     <li>Creates under-replicated topics by scaling the replication factor
     *     beyond the default broker replica count.</li>
     *     <li>Creates unavailable topics to simulate error scenarios.</li>
     * </ul>
     *
     * <p>Topic creation and message production are performed using Kubernetes
     * resources and Kafka clients authenticated via SCRAM-SHA.</p>
     *
     * <p>This setup ensures that subsequent UI tests can validate:</p>
     * <ul>
     *     <li>Total topic counts</li>
     *     <li>Replicated vs. under-replicated vs. unavailable topic states</li>
     *     <li>Correct topic status reporting in both Overview and Topics pages</li>
     *     <li>Handling of managed and unmanaged topics</li>
     * </ul>
     *
     * @see TopicChecks
     * @see KafkaTopicUtils
     * @see KafkaClientsBuilder
     * @see WaitUtils
     */
    @SetupTestBucket(ALL_TOPIC_TYPES_BUCKET)
    public void prepareAllTopicTypes() {
        LOGGER.info("Check default UI state before preparing test topics");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0);

        LOGGER.info("Create all types of topics");
        final int scaledUpBrokerReplicas = Constants.REGULAR_BROKER_REPLICAS + 1;

        List<KafkaTopic> replicatedTopics = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, true, 1, 1, 1);
        // Produce extra messages for the last fullyReplicated topic - use higher message count number to take more storage
        String topicWithMoreMessages = replicatedTopics.get(REPLICATED_TOPICS_COUNT - 1).getMetadata().getName();
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicWithMoreMessages)
            .withMessageCount(Constants.MESSAGE_COUNT_HIGH)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicWithMoreMessages))
            .withConsumerName(KafkaNamingUtils.consumerName(topicWithMoreMessages))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicWithMoreMessages))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();
        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        KafkaTopicUtils.setupUnmanagedTopicsAndReturnNames(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.messageCount(), 1, 1, 1);
        KafkaTopicUtils.setupUnderReplicatedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.messageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas);
        KafkaTopicUtils.setupUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.messageCount(), 1, 1, 1);
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
                .editSpec()
                    .withKafkaClusters(new KafkaClusterBuilder()
                        .withId(tcc.kafkaName())
                        .withName(tcc.kafkaName())
                        .withListener(Constants.SECURE_LISTENER_NAME)
                        .withNamespace(tcc.namespace()).build())
                .endSpec()
            .build());
        PwUtils.loginWithKafkaCredentials(tcc, tcc.namespace(), tcc.kafkaUserName());
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
