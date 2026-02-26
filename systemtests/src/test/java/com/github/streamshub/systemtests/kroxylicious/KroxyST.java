package com.github.streamshub.systemtests.kroxylicious;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.kroxylicious.KroxyResourcesSetup;
import com.github.streamshub.systemtests.setup.kroxylicious.KroxyliciousOperatorSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kroxy.KroxyUtils;
import com.github.streamshub.systemtests.utils.testchecks.KroxyChecks;
import com.github.streamshub.systemtests.utils.testutils.KroxyTestUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

@Tag(TestTags.REGRESSION)
public class KroxyST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KroxyST.class);

    protected TestCaseConfig tcc;
    protected KroxyliciousOperatorSetup kroxyOperatorSetup;

    /**
     * Verifies that both the default Kafka cluster (from Strimzi CR)
     * and the virtual Kafka cluster (via Kroxy) are correctly displayed
     * in the Console UI.
     *
     * <p>The test performs the following validations:
     * <ul>
     *     <li>Default Kafka cluster overview displays correct name, version, and broker count.</li>
     *     <li>Total available Kafka cluster count is correct.</li>
     *     <li>Kafka cluster selection dropdown contains both physical and virtual clusters.</li>
     *     <li>After switching to the virtual cluster, its overview displays
     *         the correct name, normalized version, and broker count.</li>
     * </ul>
     */
    @Test
    void testDisplayVirtualCluster() {
        LOGGER.info("Verify default kafka from strimzi CR");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_NAME, tcc.kafkaName(), true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_KAFKA_VERSION, Environment.ST_KAFKA_VERSION, true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS, true);

        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "2", true);
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_NAV_KAFKA_SELECT_BUTTON);
        KroxyChecks.checkKafkaClusterDropdownContains(tcc, List.of(tcc.kafkaName(), tcc.virtualKafkaClusterName()));

        LOGGER.info("Verify virtual kafka from kroxy");
        PwUtils.login(tcc, tcc.virtualKafkaClusterName());

        String virtualClusterVersion = KroxyTestUtils.normalizeVirtualClusterVersionToMajorMinor(Environment.ST_KAFKA_VERSION);

        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_NAME, tcc.virtualKafkaClusterName(), true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_KAFKA_VERSION, virtualClusterVersion, true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS, true);

        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_TOTAL_AVAILABLE_KAFKA_COUNT, "2", true);
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_NAV_KAFKA_SELECT_BUTTON);
        KroxyChecks.checkKafkaClusterDropdownContains(tcc, List.of(tcc.kafkaName(), tcc.virtualKafkaClusterName()));
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        kroxyOperatorSetup = new KroxyliciousOperatorSetup(Constants.CO_NAMESPACE);
        kroxyOperatorSetup.setup();

        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());

        KroxyResourcesSetup.setupDefaultProxyResourcesWithAuthIfNeeded(tcc.namespace(), tcc.kafkaName(), tcc.kafkaUserName(),
            tcc.kafkaProxyName(), tcc.kafkaProxyIngressName(), tcc.kafkaServiceName(), tcc.virtualKafkaClusterName(), tcc.kafkaProtocolFilterName());

        // Must be without namespace, otherwise console looks up kafka CR
        //
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
                .editSpec()
                    .addNewKafkaCluster()
                        .withId(tcc.virtualKafkaClusterName())
                        .withName(tcc.virtualKafkaClusterName())
                        .withNewProperties()
                            .addNewValue()
                                .withName(Constants.BOOTSTRAP_SERVERS)
                                .withValue(KroxyUtils.getKroxyBootstrapServer(tcc.namespace(), tcc.virtualKafkaClusterName(), tcc.kafkaProxyIngressName()))
                            .endValue()
                        .endProperties()
                    .endKafkaCluster()
                .endSpec()
            .build());

        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
        kroxyOperatorSetup.teardown();
    }
}
