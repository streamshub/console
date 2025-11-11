package com.github.streamshub.systemtests.auth;

import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.AuthTestConstants;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.KafkaDashboardPageSelectors;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import com.github.streamshub.systemtests.utils.testutils.AuthTestSetupUtils;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Base64;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(TestTags.REGRESSION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(AuthST.class);
    private TestCaseConfig tcc;

    // Keycloak
    private final KeycloakSetup keycloakSetup = new KeycloakSetup(Constants.KEYCLOAK_NAMESPACE);
    protected KeycloakConfig keycloakConfig;

    @Order(1)
    @Test
    void testAccessDevUser() {
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_DEV_BOB, AuthTestConstants.USER_DEV_BOB);
        // Check correct user is logged in
        tcc.page().navigate(ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_DEV_BOB, true);

        LOGGER.info("Check dashboard with list of available kafkas");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_DEV_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Check available kafka");
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data are still correct");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_DEV_BOB, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Verify topic display");
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify that Admin Kafka topics won't appear in search");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        // Logout and check user is no longer logged in
        PwUtils.logoutUser(tcc, AuthTestConstants.USER_DEV_BOB);
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true));
    }

    @Order(2)
    @Test
    void testAccessAdminUser() {
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_ADMIN_ALICE, AuthTestConstants.USER_ADMIN_ALICE);
        // Check correct user is logged in
        tcc.page().navigate(ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_ADMIN_ALICE, true);

        LOGGER.info("Check dashboard with list of available kafkas");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 2, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Check Dev kafka");
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data are correct");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_ADMIN_ALICE, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "2", true);

        LOGGER.info("Verify topic display of Dev Kafka");
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify Dev Kafka does not have topics from Admin kafka");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        LOGGER.info("Check Admin kafka");
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data are still correct");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_ADMIN_ALICE, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "2", true);

        LOGGER.info("Verify topic display of Admin Kafka");
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify Admin Kafka does not have topics from Dev kafka");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved from Admin Kafka", AuthTestConstants.TEAM_DEV_TOPIC_PREFIX);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        LOGGER.debug("Verify topic name containing {} can be retrieved", AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        // Logout and check user is no longer logged in
        PwUtils.logoutUser(tcc, AuthTestConstants.USER_ADMIN_ALICE);
        // Dev
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true));
        // Admin
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true));
    }

    @Order(3)
    @Test
    void testAccessTopicsViewUser() {
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_TOPICONLY_FRANK, AuthTestConstants.USER_TOPICONLY_FRANK);

        // Check correct user is logged in
        tcc.page().navigate(ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_TOPICONLY_FRANK, true);

        LOGGER.info("Check dashboard with list of available kafkas");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_DEV_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Verify developers Kafka cluster is accessible");
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));

        LOGGER.info("Check navbar data are correct");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_TOPICONLY_FRANK, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Verify topic count display");
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify Topics");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        LOGGER.info("Verify Nodes page is unavailable");
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "Not Authorized", true);

        LOGGER.info("Verify consumer groups page is unavailable");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, ""), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "Not Authorized", true);
    }

    // Must be the last test
    @Order(Integer.MAX_VALUE)
    @Test
    void testAccessConsumerGroupsViewUser() {
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_CONSUMERONLY_GRACE, AuthTestConstants.USER_CONSUMERONLY_GRACE);

        // Check correct user is logged in
        tcc.page().navigate(ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check navbar data");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_CONSUMERONLY_GRACE, true);

        LOGGER.info("Check dashboard with list of available kafkas");
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, AuthTestConstants.TEAM_DEV_KAFKA_NAME, true);
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, true);

        LOGGER.info("Verify developers Kafka cluster is accessible");
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME));

        LOGGER.info("Check navbar data are correct");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_CONSUMERONLY_GRACE, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "1", true);

        LOGGER.info("Verify topic count display is unavailable");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD, "Not Authorized", true);

        LOGGER.info("Verify Topics page is unavailable");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "Not Authorized", true);

        LOGGER.info("Verify Nodes page is unavailable");
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_NOT_AUTHORIZED_CONTENT, "Not Authorized", true);

        LOGGER.info("Verify consumer groups page is available");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, ""), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT, "No consumer groups", true);

        String newTopicName = AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + "continuous-msg";
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

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, ""), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_TABLE, KafkaNamingUtils.consumerGroupName(newTopicName), true);

        WaitUtils.waitForClientsSuccess(clients);
    }

    @BeforeAll
    void testClassSetup() {
        // Setup keycloak operator
        keycloakConfig = keycloakSetup.setupKeycloakAndReturnConfig();

        // Setup namespace and kafka + console instance
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());

        // Setup Kafkas for both teams
        // Dev Kafka
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME,
            AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX,
            AuthTestConstants.DEV_REPLICATED_TOPICS_COUNT, true, 1, 1, 1);

        // Admin Kafka
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX,
            AuthTestConstants.ADMIN_REPLICATED_TOPICS_COUNT, true, 1, 1, 1);

        // Import console auth realm
        KeycloakSetup.importConsoleRealm(keycloakConfig, "https://" + tcc.consoleInstanceName() + "." + ClusterUtils.getClusterDomain());

        //Secret to truststore
        KubeResourceManager.get().createOrUpdateResourceWithWait(new SecretBuilder()
            .withNewMetadata()
                .withName(Constants.KEYCLOAK_TRUST_STORE_ACCCESS_SECRET_NAME)
                .withNamespace(tcc.namespace())
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToData(Constants.PASSWORD_KEY_NAME, Base64.getEncoder().encodeToString(Constants.TRUST_STORE_PASSWORD.getBytes()))
            .build());

        // Configmap with truststore
        KubeResourceManager.get().createOrUpdateResourceWithWait(new ConfigMapBuilder()
            .withNewMetadata()
                .withName(Constants.KEYCLOAK_TRUST_STORE_CONFIGMAP_NAME)
                .withNamespace(tcc.namespace())
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToBinaryData(Constants.TRUST_STORE_KEY_NAME, Base64.getEncoder().encodeToString(FileUtils.readFileBytes(Constants.TRUST_STORE_FILE_PATH)))
            .build());

        // Console instance
        ConsoleInstanceSetup.setupIfNeeded(AuthTestSetupUtils.getOidcConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), keycloakConfig));
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
