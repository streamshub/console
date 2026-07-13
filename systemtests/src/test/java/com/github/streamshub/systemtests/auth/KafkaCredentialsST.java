package com.github.streamshub.systemtests.auth;

import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
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
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
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
     *   <li>Access to the cluster Overview page, confirming exactly one Kafka cluster
     *       is listed as available and that the broker replica count card shows
     *       {@code REGULAR_BROKER_REPLICAS}/{@code REGULAR_BROKER_REPLICAS} (3/3).</li>
     *   <li>Proper rendering of the Nodes page, including:
     *       <ul>
     *           <li>Total node count of 6 (3 brokers + 3 controllers)</li>
     *           <li>Working node count matching the total, with 0 nodes in a warning state</li>
     *           <li>Broker and controller role distribution (3 controllers, 3 brokers)</li>
     *       </ul>
     *   </li>
     *   <li>Correct display of topic statistics (14 total topics, 9 fully/unmanaged
     *       replicated, 3 under-replicated, 2 unavailable) on both the Overview and
     *       Topics pages.</li>
     *   <li>Display of the authenticated Kafka username ({@code tcc.kafkaUserName()})
     *       in the navigation bar.</li>
     *   <li>Pause and resume of Kafka reconciliation via the UI, including:
     *       <ul>
     *           <li>Validation of the confirmation modal content (header, body,
     *               "Cancel"/"Confirm" buttons)</li>
     *           <li>Verification of the "reconciliation paused" notification after confirming</li>
     *           <li>Verification that the underlying Kafka resource's
     *               {@code strimzi.io/pause-reconciliation} annotation flips to
     *               {@code "true"} on pause and back to {@code "false"} on resume</li>
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
        LOGGER.info("Starting Kafka credentials UI test for user '{}' on Kafka cluster '{}'", tcc.kafkaUserName(), tcc.kafkaName());
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        // Verify basic UI elements
        LOGGER.info("Verifying navbar shows {} available Kafka cluster(s)", 1);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Verifying broker replica count card shows {}/{}", Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_BROKER_REPLICAS);
        PwUtils.waitForContainsText(tcc,
            ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS, TimeConstants.ACTION_WAIT_LONG);

        LOGGER.info("Navigating to Nodes page to verify node counts and role distribution");
        PwUtils.navigate(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));

        // Header
        LOGGER.debug("Checking Nodes page header badges - total: {}, working: {}, warning: 0", Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_TOTAL_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT, "0", true);
        // Page infobox
        LOGGER.debug("Checking Nodes page table row count matches total node count {}", Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS);
        PwUtils.waitForLocatorCount(tcc, Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, NodesPageSelectors.NPS_TABLE_BODY, true);
        // total nodes
        LOGGER.debug("Checking node role distribution - {} controllers, {} brokers", Constants.REGULAR_CONTROLLER_REPLICAS, Constants.REGULAR_BROKER_REPLICAS);
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(1).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with controller role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(2).build(), Integer.toString(Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with broker role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(3).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS), true);
        // Node table
        LOGGER.debug("Asserting Nodes page table contains {} rows", Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS);
        assertEquals(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, tcc.page().locator(NodesPageSelectors.NPS_TABLE_BODY).all().size());

        LOGGER.info("Verifying navbar displays logged-in Kafka username '{}'", tcc.kafkaUserName());
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, tcc.kafkaUserName(), true);

        LOGGER.info("Verifying topic counts on Overview and Topics pages - total: {}, replicated: {}, under-replicated: {}, unavailable: {}", TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        // Test pausing kafka reconciliation to confirm UI communicates with kafka
        LOGGER.info("Pausing Kafka reconciliation for cluster '{}' via UI", tcc.kafkaName());
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        LOGGER.debug("Clicking pause reconciliation button to open confirmation modal");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, "Pause Reconciliation", false);
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        LOGGER.debug("Verifying pause reconciliation confirmation modal content");
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.PAGES_POPUP_MODAL);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_HEADER, "Pause cluster reconciliation?", false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_BODY, "While paused, updates to the cluster are ignored until reconciliation is resumed.", false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, "Cancel", false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, "Confirm", false);

        LOGGER.debug("Clicking Confirm button to pause reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        // Check aftermath
        LOGGER.info("Verifying reconciliation paused notification is displayed");
        PwUtils.waitForLocatorVisible(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION, "Cluster reconciliation paused. Changes to the Kafka resource will not be applied.", false);

        LOGGER.info("Verifying Kafka resource '{}' annotation '{}' flips to '{}'", tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        LOGGER.info("Resuming Kafka reconciliation for cluster '{}' via UI", tcc.kafkaName());
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, "Resume Reconciliation", true);
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        LOGGER.debug("Verifying resume reconciliation confirmation modal content");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, "Cancel", false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, "Confirm", false);

        LOGGER.debug("Clicking Confirm button to resume reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        // Reconciliation is resumed and button should display Pause
        LOGGER.info("Verifying reconciliation resumed and button displays 'Pause Reconciliation' again");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, "Pause Reconciliation", true);

        // Check annotation
        LOGGER.info("Verifying Kafka resource '{}' annotation '{}' flips back to '{}'", tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
        // TODO: remove once fixed
        //PwUtils.logoutUser(tcc, tcc.kafkaUserName(), false);
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
     *     <li>Creates 5 fully replicated (managed) topics ({@code REPLICATED_TOPICS_COUNT}).</li>
     *     <li>Produces 10 000 messages ({@code Constants.MESSAGE_COUNT_HIGH}) to the last
     *     replicated topic to increase storage usage and simulate realistic load.</li>
     *     <li>Creates 4 unmanaged replicated topics ({@code UNMANAGED_REPLICATED_TOPICS_COUNT}).</li>
     *     <li>Creates 3 under-replicated topics by scaling the replication and min in-sync
     *     replica factor to 4 (one more than the default 3 broker replicas).</li>
     *     <li>Creates 2 unavailable topics to simulate error scenarios.</li>
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
        LOGGER.info("Verifying UI shows 0 topics before creating test topics for bucket '{}'", ALL_TOPIC_TYPES_BUCKET);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0);

        LOGGER.info("Creating {} total test topics across replicated, unmanaged, under-replicated and unavailable types", TOTAL_TOPICS_COUNT);
        final int scaledUpBrokerReplicas = Constants.REGULAR_BROKER_REPLICAS + 1;

        LOGGER.info("Creating {} replicated (managed) topics with prefix '{}'", REPLICATED_TOPICS_COUNT, Constants.REPLICATED_TOPICS_PREFIX);
        List<KafkaTopic> replicatedTopics = KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, 1, 1, 1);
        // Produce extra messages for the last fullyReplicated topic - use higher message count number to take more storage
        String topicWithMoreMessages = replicatedTopics.get(REPLICATED_TOPICS_COUNT - 1).getMetadata().getName();
        LOGGER.info("Producing {} messages to topic '{}' to increase storage usage", Constants.MESSAGE_COUNT_HIGH, topicWithMoreMessages);
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
        LOGGER.debug("Waiting for producer/consumer clients to finish sending messages to topic '{}'", topicWithMoreMessages);
        WaitUtils.waitForClientsSuccess(clients);

        LOGGER.info("Creating {} unmanaged replicated topics with prefix '{}'", UNMANAGED_REPLICATED_TOPICS_COUNT, Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX);
        KafkaTopicUtils.setupUnmanagedTopicsAndReturnNames(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1);
        LOGGER.info("Creating {} under-replicated topics with prefix '{}' using replication factor {}", UNDER_REPLICATED_TOPICS_COUNT, Constants.UNDER_REPLICATED_TOPICS_PREFIX, scaledUpBrokerReplicas);
        KafkaTopicUtils.setupUnderReplicatedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas);
        LOGGER.info("Creating {} unavailable topics with prefix '{}'", UNAVAILABLE_TOPICS_COUNT, Constants.UNAVAILABLE_TOPICS_PREFIX);
        KafkaTopicUtils.setupUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1);
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
