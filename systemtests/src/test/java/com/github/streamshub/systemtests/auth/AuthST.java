package com.github.streamshub.systemtests.auth;

import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.AuthTestConstants;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.testutils.AuthTestSetupUtils;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
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
    void testAccessOfDevUser() {
        PwUtils.loginWithOidcUser(tcc, AuthTestConstants.USER_DEV_BOB, AuthTestConstants.USER_DEV_BOB);
        // Check correct user is logged in
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, AuthTestConstants.TEAM_DEV_KAFKA_NAME), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON, AuthTestConstants.USER_DEV_BOB, true);
        // Check list of displayed kafkas
            // KDPS_KAFKA_CLUSTER_LIST_ITEMS == only one kafka name


        // Enter kafka, verify topics are there
            // PAGES_TOTAL_AVAILABLE_KAFKA_COUNT == 1
            // PAGES_LEFT_TOOLBAR_KAFKA_NAME == kafkaname

        // Logout and check going back that its not possible
        PwUtils.logoutUser(tcc, AuthTestConstants.USER_DEV_BOB);

    }





    @BeforeAll
    void testClassSetup() {
        // Setup keycloak operator
        keycloakConfig = keycloakSetup.setupKeycloakAndReturnConfig();

        // Setup namespace and kafka + console instance
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());

        // Setup Kafkas for both teams
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_DEV_KAFKA_NAME);
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), AuthTestConstants.TEAM_ADMIN_KAFKA_NAME);

        // Import console auth realm
        KeycloakSetup.importConsoleRealm(keycloakConfig, "https://" + tcc.consoleInstanceName() + "." + ClusterUtils.getClusterDomain());

        // Secret to truststore
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
