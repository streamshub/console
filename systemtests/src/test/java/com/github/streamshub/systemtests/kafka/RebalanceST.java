package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
public class RebalanceST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(RebalanceST.class);

    @Test
    void testKafkaRebalance() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int imbalancedPartitions = 20;

        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, 3, true, 1, 1, 1);
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), "rebalance-topic", 1, true, imbalancedPartitions, 1, 1);
        KubeResourceManager.get().createOrUpdateResourceWithWait(KafkaSetup.getKafkaRebalance(tcc.namespace(), tcc.kafkaName()).build());
        LOGGER.info("Verify rebalance proposals");
        tcc.page().navigate(PwPageUrls.getKafkaRebalancePage(tcc, tcc.kafkaName()));
        PwUtils.waitForLocatorCount(tcc, 1, NodesPageSelectors.NPS_REBALANCE_TABLE_ITEMS, true);
        assertTrue(tcc.page().locator(new CssBuilder(NodesPageSelectors.NPS_REBALANCE_PROPOSAL_NAME).nth()));

    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupKafkaWithCcIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
