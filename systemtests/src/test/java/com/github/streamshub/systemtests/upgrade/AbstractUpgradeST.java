package com.github.streamshub.systemtests.upgrade;

import com.github.streamshub.systemtests.ResetTracingExtension;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.TestExecutionWatcher;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.interfaces.BucketMethodsOrderRandomizer;
import com.github.streamshub.systemtests.interfaces.ExtensionContextParameterResolver;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.strimzi.StrimziOperatorSetup;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.skodjob.kubetest4j.annotations.ResourceManager;
import io.skodjob.kubetest4j.annotations.TestVisualSeparator;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;


@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ResourceManager(cleanResources = false)
@SuppressWarnings("ClassDataAbstractionCoupling")
@ExtendWith({TestExecutionWatcher.class, ResetTracingExtension.class})
@ExtendWith(ExtensionContextParameterResolver.class)
@TestMethodOrder(BucketMethodsOrderRandomizer.class)
public class AbstractUpgradeST {
    private static final Logger LOGGER = LogWrapper.getLogger(AbstractUpgradeST.class);
    private static boolean initialized = false;
    protected final StrimziOperatorSetup strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);

    // Topics - shared by OlmUpgradeST and YamlUpgradeST, both pre-create the same 17 topics
    // (12 fully replicated, 3 under-replicated, 2 unavailable) to verify against before/after their upgrade.
    protected static final int REPLICATED_TOPICS_COUNT = 7;
    protected static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 5;
    protected static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    protected static final int UNDER_REPLICATED_TOPICS_COUNT = 3;
    protected static final int UNAVAILABLE_TOPICS_COUNT = 2;
    protected static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;

    // Old (pre-redesign) Console UI, valid for every operator version that predates the UI overhaul
    // that landed after 0.12.3 (e.g. 0.11.0, 0.12.3) - shared by OlmUpgradeST and YamlUpgradeST for
    // whichever side(s) of their respective upgrades still target that old UI.
    protected static final String COPS_TOPICS_CARD_TOTAL_TOPICS = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div > div.pf-v6-l-flex > div:nth-of-type(1) > div.pf-v6-c-content";
    protected static final String COPS_TOPICS_CARD_TOTAL_PARTITIONS = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-content";
    protected static final String COPS_TOPICS_CARD_FULLY_REPLICATED = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(2) > div:nth-of-type(1)";
    protected static final String COPS_TOPICS_CARD_UNDER_REPLICATED = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(2) > div:nth-of-type(2)";
    protected static final String COPS_TOPICS_CARD_UNAVAILABLE = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(2) > div:nth-of-type(3)";
    protected static final String TPS_HEADER_TOTAL_TOPICS_BADGE = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(2) > span.pf-v6-c-label > span.pf-v6-c-label__content > span";
    protected static final String TPS_HEADER_BADGE_STATUS_SUCCESS = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(3) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";
    protected static final String TPS_HEADER_BADGE_STATUS_WARNING = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(4) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";
    protected static final String TPS_HEADER_BADGE_STATUS_ERROR = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(5) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";

    @BeforeAll
    void setupTestSuite(ExtensionContext extensionContext) {
        if (!initialized) {
            SetupUtils.initializeSystemTests();
            initialized = true;
        }

        KubeResourceManager.get().setTestContext(extensionContext);
        NamespaceUtils.prepareNamespace(Constants.CO_NAMESPACE);
        // V1 not compatible with 0.12 console
        strimziOperatorSetup.install("0.51.0");
    }

    @BeforeEach
    void setupTestCase(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        ClusterUtils.checkClusterHealth();
    }

    @AfterEach
    void teardownTestCase(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        SetupUtils.cleanupIfNeeded();
    }

    @AfterAll
    void teardownTestSuite(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        SetupUtils.cleanupIfNeeded();
    }

    /**
     * Checks Overview/Topics page topic counts using the pre-redesign Console UI's DOM. Bypasses
     * {@link PwUtils#login(TestCaseConfig)} and {@link com.github.streamshub.systemtests.utils.testchecks.TopicChecks},
     * which target the current, redesigned UI and don't match an old version's markup.
     *
     * <p>Shared by {@link OlmUpgradeST} and {@link YamlUpgradeST} for whichever side(s) of their
     * respective upgrades still target the old UI.
     *
     * @param tcc                        the test case configuration
     * @param totalTopicsCount           expected total topic count
     * @param totalReplicatedTopicsCount expected fully-replicated topic count
     * @param underReplicatedTopicsCount expected under-replicated topic count
     * @param unavailableTopicsCount     expected unavailable topic count
     */
    protected void checkOldUiTopicState(TestCaseConfig tcc, int totalTopicsCount, int totalReplicatedTopicsCount, int underReplicatedTopicsCount, int unavailableTopicsCount) {
        // Old (pre-redesign) UI had a dedicated sign in with credentials button, no login modal
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), false, false);
        PwUtils.waitForLocatorAndClick(tcc, "body > div > div > div > div > form > button");

        LOGGER.info("Checking overview page topic status: {} total topics, {} partitions, {} fully replicated, {} under-replicated, {} unavailable",
            totalTopicsCount, totalTopicsCount, totalReplicatedTopicsCount, underReplicatedTopicsCount, unavailableTopicsCount);
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_TOTAL_TOPICS, totalTopicsCount + " topics", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_TOTAL_PARTITIONS, totalTopicsCount + " partitions", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_FULLY_REPLICATED, totalReplicatedTopicsCount + " Fully replicated", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_UNDER_REPLICATED, underReplicatedTopicsCount + " Under replicated", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_UNAVAILABLE, unavailableTopicsCount + " Unavailable", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);

        LOGGER.info("Checking topics page topic status: {} total topics, {} fully replicated, {} under-replicated, {} unavailable",
            totalTopicsCount, totalReplicatedTopicsCount, underReplicatedTopicsCount, unavailableTopicsCount);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, TPS_HEADER_TOTAL_TOPICS_BADGE, totalTopicsCount + " total", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, TPS_HEADER_BADGE_STATUS_SUCCESS, Integer.toString(totalReplicatedTopicsCount), false, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, TPS_HEADER_BADGE_STATUS_WARNING, Integer.toString(underReplicatedTopicsCount), false, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, TPS_HEADER_BADGE_STATUS_ERROR, Integer.toString(unavailableTopicsCount), false, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
    }
}
