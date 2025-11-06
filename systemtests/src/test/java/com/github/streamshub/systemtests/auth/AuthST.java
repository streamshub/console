package com.github.streamshub.systemtests.auth;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.AuthTestConstants;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.KafkaDashboardPageSelectors;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
    private static final int DEV_REPLICATED_TOPICS_COUNT = 3;
    private static final int ADMIN_REPLICATED_TOPICS_COUNT = 5;


    @Order(1)
    @Test
    void testAccessOfDevUser() {
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
        TopicChecks.checkOverviewPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, DEV_REPLICATED_TOPICS_COUNT, DEV_REPLICATED_TOPICS_COUNT, DEV_REPLICATED_TOPICS_COUNT, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME, DEV_REPLICATED_TOPICS_COUNT, DEV_REPLICATED_TOPICS_COUNT, 0, 0);

        LOGGER.info("Verify that Admin Kafka topics won't appear in search");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);

        LOGGER.debug("Verify topic name containing {} cannot be retrieved", AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 0, TopicsPageSelectors.TPS_TABLE_ROWS, false);

        LOGGER.info("Verify consumer groups");

        // Logout and check user is no longer logged in
        PwUtils.logoutUser(tcc, AuthTestConstants.USER_DEV_BOB);
        tcc.page().navigate(PwPageUrls.getKafkaBaseUrl(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        assertNotEquals(tcc.page().url(), ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true));
    }

    @BeforeAll
    void testClassSetup() {
        // Setup keycloak operator
        keycloakConfig = keycloakSetup.setupKeycloakAndReturnConfig();

        // Setup namespace and kafka + console instance
        tcc = getTestCaseConfig();
        //NamespaceUtils.prepareNamespace(tcc.namespace());

        // // Setup Kafkas for both teams
        // // Dev Kafka
        // KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        // KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME,
        //     AuthTestConstants.TEAM_DEV_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX,
        //     DEV_REPLICATED_TOPICS_COUNT, true, 1, 1, 1);
        //
        // // Admin Kafka
        // KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);
        // KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.TEAM_ADMIN_TOPIC_PREFIX + Constants.REPLICATED_TOPICS_PREFIX,
        //     ADMIN_REPLICATED_TOPICS_COUNT, true, 1, 1, 1);

        // Import console auth realm
        //KeycloakSetup.importConsoleRealm(keycloakConfig, "https://" + tcc.consoleInstanceName() + "." + ClusterUtils.getClusterDomain());

        // Secret to truststore
        // KubeResourceManager.get().createOrUpdateResourceWithWait(new SecretBuilder()
        //     .withNewMetadata()
        //         .withName(Constants.KEYCLOAK_TRUST_STORE_ACCCESS_SECRET_NAME)
        //         .withNamespace(tcc.namespace())
        //         .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
        //     .endMetadata()
        //     .addToData(Constants.PASSWORD_KEY_NAME, Base64.getEncoder().encodeToString(Constants.TRUST_STORE_PASSWORD.getBytes()))
        //     .build());
        //
        // // Configmap with truststore
        // KubeResourceManager.get().createOrUpdateResourceWithWait(new ConfigMapBuilder()
        //     .withNewMetadata()
        //         .withName(Constants.KEYCLOAK_TRUST_STORE_CONFIGMAP_NAME)
        //         .withNamespace(tcc.namespace())
        //         .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
        //     .endMetadata()
        //     .addToBinaryData(Constants.TRUST_STORE_KEY_NAME, Base64.getEncoder().encodeToString(FileUtils.readFileBytes(Constants.TRUST_STORE_FILE_PATH)))
        //     .build());

        // Console instance
        //ConsoleInstanceSetup.setupIfNeeded(AuthTestSetupUtils.getOidcConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), keycloakConfig));
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
