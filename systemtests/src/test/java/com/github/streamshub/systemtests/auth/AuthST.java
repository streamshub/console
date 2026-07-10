package com.github.streamshub.systemtests.auth;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.AuthTestConstants;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.GroupsPageSelectors;
import com.github.streamshub.systemtests.locators.KafkaDashboardPageSelectors;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakInstanceSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakOperatorSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakTestConfig;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(AuthST.class);
    private TestCaseConfig tcc;

    // Keycloak
    protected KeycloakOperatorSetup keycloakOperator;
    protected KeycloakInstanceSetup keycloakInstance;

    /**
     * Verifies that a developer user can log in via OIDC and access their assigned Kafka cluster.
     *
     * <p>The test covers end-to-end behavior for a dev user, including UI navigation, topic visibility,
     * and filtering behavior:</p>
     *
     * <ul>
     *   <li>Logs in as developer user "bob" and checks that the navbar correctly displays the logged-in username.</li>
     *   <li>Validates that the user can see only the single Kafka cluster assigned to their team ({@code kc-team-dev})
     *       and that the total count is correct.</li>
     *   <li>Navigates to the Kafka overview and topics pages, verifying that all 3 (dev) replicated topics are
     *       reported as fully replicated and available.</li>
     *   <li>Ensures that topics belonging to the admin Kafka cluster (prefixed {@code admin-replicated}) are not
     *       visible in search results for this user.</li>
     * </ul>
     *
     * <p>This test ensures that access control, topic visibility, and UI elements behave correctly
     * for a developer user, confirming both security restrictions and correct data display.</p>
     */
    @Order(1)
    @Test
    void testAccessDevUser() {
        LOGGER.info("Verify dev user '{}' can access assigned Kafka cluster '{}'", AuthTestConstants.USER_DEV_BOB, AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_DEV_BOB, AuthTestConstants.USER_DEV_BOB);
        // Check correct user is logged in
        PwUtils.navigate(tcc, ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true));

        LOGGER.info("Verify navbar displays logged-in user '{}'", AuthTestConstants.USER_DEV_BOB);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_DEV_BOB, true);

        LOGGER.info("Verify dashboard lists a single Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_DEV_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Open Kafka cluster detail view from the dashboard list");
        PwUtils.waitForLocatorAndClick(tcc, KafkaDashboardPageSelectors.getViewButton(1));


        LOGGER.info("Verify navbar still shows user '{}' after opening the Kafka cluster view", AuthTestConstants.USER_DEV_BOB);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_DEV_BOB, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Verify topic replication state on overview and topics pages for Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify that Admin Kafka cluster '{}' topics won't appear in search", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_INPUT, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_NO_RESULTS_FOUND, "No results found", false);

        // TODO: enable once fixed
        // // Logout and check user is no longer logged in
        // PwUtils.logoutUser(tcc, AuthTestConstants.USER_DEV_BOB, true);
        // PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        // Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        // assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true));
    }

    /**
     * Verifies that an admin user can log in via OIDC and access all assigned Kafka clusters.
     *
     * <p>The test covers end-to-end behavior for an admin user, including UI navigation, topic visibility,
     * and filtering behavior across multiple Kafka clusters:</p>
     *
     * <ul>
     *   <li>Logs in as admin user "alice" and checks that the navbar correctly displays the logged-in username.</li>
     *   <li>Validates that the user can see both Kafka clusters ({@code kc-team-dev} and {@code kc-team-admin})
     *       and that the total count is correct.</li>
     *   <li>Navigates to the dev Kafka cluster and verifies that all 3 (dev) replicated topics are reported as
     *       fully replicated and available, and that admin cluster topics (prefixed {@code admin-replicated})
     *       cannot be found via search.</li>
     *   <li>Navigates to the admin Kafka cluster and verifies that all 5 (admin) replicated topics are reported as
     *       fully replicated and available, that dev cluster topics (prefixed {@code dev-replicated}) cannot be
     *       found via search, and that admin topics can be found via search.</li>
     * </ul>
     *
     * <p>This test ensures that admin access control, topic visibility, and UI elements behave correctly,
     * confirming both security restrictions and full access to all relevant Kafka resources.</p>
     */
    @Order(2)
    @Test
    void testAccessAdminUser() {
        LOGGER.info("Verify admin user '{}' can access both Kafka clusters '{}' and '{}'", AuthTestConstants.USER_ADMIN_ALICE, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_ADMIN_ALICE, AuthTestConstants.USER_ADMIN_ALICE);
        // Check correct user is logged in
        PwUtils.navigate(tcc, ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true));

        LOGGER.info("Verify navbar displays logged-in user '{}'", AuthTestConstants.USER_ADMIN_ALICE);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_ADMIN_ALICE, true);

        LOGGER.info("Verify dashboard lists both Kafka clusters '{}' and '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 2, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Navigate to Dev Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));

        LOGGER.info("Verify navbar shows user '{}' and total Kafka count on Dev cluster page", AuthTestConstants.USER_ADMIN_ALICE);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_ADMIN_ALICE, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "2", true);

        LOGGER.info("Verify topic replication state on overview and topics pages for Dev Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify Dev Kafka cluster '{}' does not expose topics from Admin cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_INPUT, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_NO_RESULTS_FOUND, "No results found", false);

        LOGGER.info("Navigate to Admin Kafka cluster '{}'", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME));

        LOGGER.info("Verify navbar shows user '{}' and total Kafka count on Admin cluster page", AuthTestConstants.USER_ADMIN_ALICE);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_ADMIN_ALICE, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "2", true);

        LOGGER.info("Verify topic replication state on overview and topics pages for Admin Kafka cluster '{}'", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify Admin Kafka cluster '{}' does not expose topics from Dev cluster '{}'", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME));
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved from Admin Kafka", AuthTestConstants.TEAM_DEV_TOPIC_PREFIX);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_INPUT, AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_NO_RESULTS_FOUND, "No results found", false);

        LOGGER.info("Verify Admin Kafka cluster '{}' topics matching prefix '{}' can be found via search", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX);
        LOGGER.debug("Verify topic name containing {} can be retrieved", AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_INPUT, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        // TODO: enable once fixed
        // // Logout and check user is no longer logged in
        // PwUtils.logoutUser(tcc, AuthTestConstants.USER_ADMIN_ALICE, true);
        // // Dev
        // PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        // Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        // assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true));
        // // Admin
        // PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME));
        // Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        // assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true));
    }

    /**
     * Verifies that a "topics-only" usergroup can access Kafka topic information but is restricted
     * from viewing other administrative pages like Nodes and Groups.
     *
     * <p>The test covers the following behaviors:</p>
     *
     * <ul>
     *   <li>Logs in as topics-only user "frank" and confirms the navbar displays the correct username.</li>
     *   <li>Checks that the user can see only the single Kafka cluster they are authorized to access
     *       ({@code kc-team-dev}) and verifies the total count.</li>
     *   <li>Validates the overview and topics pages for the authorized dev Kafka cluster, confirming that all
     *       3 replicated topics are reported as fully replicated and available.</li>
     *   <li>Filters the Topics page by name and confirms the expected 3 replicated topics are returned.</li>
     *   <li>Verifies that the Nodes page is restricted and displays a "403" message.</li>
     * </ul>
     *
     * <p>This test ensures that topic-level access control is enforced correctly while restricting
     * access to administrative views, confirming both security and proper UI behavior.</p>
     */
    @Order(3)
    @Test
    void testAccessTopicsViewUser() {
        LOGGER.info("Verify topics-only user '{}' can view topics but not administrative pages on Kafka cluster '{}'", AuthTestConstants.USER_TOPICONLY_FRANK, AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_TOPICONLY_FRANK, AuthTestConstants.USER_TOPICONLY_FRANK);

        // Check correct user is logged in
        PwUtils.navigate(tcc, PwPageUrls.getConsoleUrl(tcc));

        LOGGER.info("Verify navbar displays logged-in user '{}'", AuthTestConstants.USER_TOPICONLY_FRANK);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_TOPICONLY_FRANK, true);

        LOGGER.info("Verify dashboard lists only the authorized Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_DEV_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Verify Dev Kafka cluster '{}' is accessible", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));

        LOGGER.info("Verify navbar shows user '{}' and total Kafka count on Dev cluster page", AuthTestConstants.USER_TOPICONLY_FRANK);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_TOPICONLY_FRANK, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Verify topic replication state on overview and topics pages for Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify topics page filtering by name prefix '{}' returns {} replicated topics", AuthTestConstants.TEAM_DEV_TOPIC_PREFIX, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_INPUT, AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        LOGGER.info("Verify Nodes page returns 403 for topics-only user '{}'", AuthTestConstants.USER_TOPICONLY_FRANK);
        PwUtils.navigate(tcc, PwPageUrls.getNodesPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "403", true);

        // TODO: enable once fixed
        // LOGGER.info("Verify consumer groups page is unavailable");
        // PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, ""));
        // PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "403 Forbidden", true);
        //
        // Logout and check user is no longer logged in
        //PwUtils.logoutUser(tcc, AuthTestConstants.USER_TOPICONLY_FRANK, true);
    }

    /**
     * Verifies that a "consumer-groups-only" user can access Kafka consumer group information
     * but is restricted from viewing topics and other administrative pages.
     *
     * <p><b>Note that this test needs to be last in execution order because it performs messaging on shared topics</b></p>
     *
     * <p>The test covers the following behaviors:</p>
     *
     * <ul>
     *   <li>Logs in as consumer-groups-only user "grace" and confirms the navbar displays the correct username.</li>
     *   <li>Checks that the user can see only the single Kafka cluster they are authorized to access
     *       ({@code kc-team-dev}) and verifies the total count.</li>
     *   <li>Verifies that the Kafka overview page reports all topic metrics (fully replicated, under-replicated,
     *       unavailable, total topics, total partitions) as "0" since topic details are not authorized for this user.</li>
     *   <li>Verifies that the Topics page displays a "Not Authorized" message and the Nodes page displays a
     *       "403 Forbidden" message.</li>
     *   <li>Verifies that the Groups page is accessible and initially shows "No groups available".</li>
     *   <li>Creates a Kafka topic ({@code dev-continuous-msg}) along with producer and consumer clients, then
     *       confirms the new consumer group appears on the Groups page.</li>
     *   <li>Waits for the producer and consumer clients to complete successfully.</li>
     * </ul>
     *
     * <p>This test ensures proper enforcement of access control for users limited to consumer-group operations,
     * confirming both UI restrictions and functional availability where permitted.</p>
     */
    @Order(Integer.MAX_VALUE)
    @Test
    void testAccessGroupsViewUser() {
        LOGGER.info("Verify consumer-groups-only user '{}' can access consumer groups but not topics on Kafka cluster '{}'", AuthTestConstants.USER_CONSUMERONLY_GRACE, AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_CONSUMERONLY_GRACE, AuthTestConstants.USER_CONSUMERONLY_GRACE);

        // Check correct user is logged in
        PwUtils.navigate(tcc, ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true));

        LOGGER.info("Verify navbar displays logged-in user '{}'", AuthTestConstants.USER_CONSUMERONLY_GRACE);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_CONSUMERONLY_GRACE, true);

        LOGGER.info("Verify dashboard lists only the authorized Kafka cluster '{}'", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_DEV_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Verify Dev Kafka cluster '{}' is accessible", AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        PwUtils.navigate(tcc, PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));

        LOGGER.info("Verify navbar shows user '{}' and total Kafka count on Dev cluster page", AuthTestConstants.USER_CONSUMERONLY_GRACE);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);
        assertTrue(tcc.page().locator(CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON).allInnerTexts().toString().contains(AuthTestConstants.USER_CONSUMERONLY_GRACE));

        LOGGER.info("Verify overview page topic metrics show 0 since topic details are not authorized");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_FULLY_REPLICATED, "0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_UNDER_REPLICATED, "0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_UNAVAILABLE, "0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_TOTAL_TOPICS, "0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_TOTAL_PARTITIONS, "0", true);

        LOGGER.info("Verify Topics page shows 'Not Authorized' for user '{}'", AuthTestConstants.USER_CONSUMERONLY_GRACE);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "Not Authorized", true);

        LOGGER.info("Verify Nodes page returns 403 Forbidden for user '{}'", AuthTestConstants.USER_CONSUMERONLY_GRACE);
        PwUtils.navigate(tcc, PwPageUrls.getNodesPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "403", true);

        LOGGER.info("Verify Groups page is accessible and initially shows no groups for user '{}'", AuthTestConstants.USER_CONSUMERONLY_GRACE);
        PwUtils.navigate(tcc, PwPageUrls.getGroupsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        PwUtils.waitForContainsText(tcc, GroupsPageSelectors.GPS_NO_GROUPS_AVAILABLE, "No groups available", true);

        String newTopicName = AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + "continuous-msg";
        LOGGER.info("Create topic '{}' with producer and consumer clients to generate a new consumer group", newTopicName);
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(newTopicName)
            .withMessageCount(Constants.MESSAGE_COUNT)
            .withDelayMs(100)
            .withProducerName(KafkaNamingUtils.producerName(newTopicName))
            .withConsumerName(KafkaNamingUtils.consumerName(newTopicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(newTopicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(AuthTestConstants.TEAM_DEV_KAFKA_NAME))
            .withUsername(KafkaNamingUtils.kafkaUserName(AuthTestConstants.TEAM_DEV_KAFKA_NAME))
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), KafkaNamingUtils.kafkaUserName(AuthTestConstants.TEAM_DEV_KAFKA_NAME), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());

        LOGGER.info("Verify consumer group '{}' appears on the Groups page", KafkaNamingUtils.consumerGroupName(newTopicName));
        PwUtils.navigate(tcc, PwPageUrls.getGroupsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));
        PwUtils.waitForContainsText(tcc, GroupsPageSelectors.GPS_TABLE, KafkaNamingUtils.consumerGroupName(newTopicName), true);

        LOGGER.info("Wait for producer and consumer clients on topic '{}' to complete successfully", newTopicName);
        WaitUtils.waitForClientsSuccess(clients);
        // TODO: enable once fixed
        // Logout and check user is no longer logged in
        //PwUtils.logoutUser(tcc, AuthTestConstants.USER_CONSUMERONLY_GRACE, true);
    }

    @BeforeAll
    void testClassSetup() {
        // Setup namespace and kafka + console instance
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        // Setup keycloak operator
        // Note from docs:
        // It is currently not fully supported for the operator to watch multiple or all namespaces.
        // In circumstances where you want to watch multiple namespaces, you can install multiple operators.
        keycloakOperator = new KeycloakOperatorSetup(tcc.namespace());
        keycloakOperator.setup();

        keycloakInstance = new KeycloakInstanceSetup(tcc.namespace());
        keycloakInstance.setup();

        // Setup Kafkas for both teams
        // Dev Kafka
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME,
            false,
            AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX,
            AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 1, 1, 1);

        // Admin Kafka
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME,
            false,
            AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX,
            AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, 1, 1, 1);

        // Import console auth realm
        keycloakInstance.importConsoleRealm(ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true),
            false,
            KeycloakTestConfig.DEFAULT_ROLE_MAPPING, KeycloakTestConfig.DEFAULT_USER_MAPPING);

        // Console instance
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getOidcConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(),
            keycloakInstance, KeycloakTestConfig.DEFAULT_ROLE_MAPPING, KeycloakTestConfig.DEFAULT_KAFKA_CLUSTERS_MAPPING).build());

        ConsoleUtils.patchConsoleNginxBuffer(tcc.namespace(), tcc.consoleInstanceName());
    }

    @AfterEach
    void testCaseTeardown() {
        tcc.resetBrowserContext();
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
