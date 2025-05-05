package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.KafkaUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaST.class);

    @Test
    void testPauseAndResumeReconciliation() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int scaledBrokersCount = 6;

        LOGGER.debug("Check that Kafka does not contain paused reconciliation");
        String reconciliationAnno = ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName())
            .getMetadata()
            .getAnnotations()
            .getOrDefault(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");

        assertEquals("false", reconciliationAnno);

        LOGGER.debug("Pause Kafka reconciliation using UI");
        PwUtils.navigateTo(tcc.page(), PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        // Open popup
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON, "Pause Reconciliation");
        tcc.page().click(CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON);
        // Assert popup
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_MODAL);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_MODAL_HEADER, "Pause cluster reconciliation?");
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_MODAL_BODY,
            "While paused, any changes to the cluster configuration will be ignored until reconciliation is resumed");
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_MODAL_CANCEL_BUTTON, "Cancel");
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_MODAL_CONFIRM_BUTTON, "Confirm");

        // Click on confirm
        tcc.page().click(CssSelectors.C_OVERVIEW_RECONCILIATION_MODAL_CONFIRM_BUTTON);
        // Assert after
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION,
            "Cluster reconciliation paused. Changes to the Kafka resource will not be applied");

        // Check Kafka
        WaitUtils.waitForKafkaAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        // Scale brokers, but expect nothing happens
        KafkaUtils.scaleBrokers(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount, false);

        // Check replicas are changed, but actual count stayed the same
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, tcc.namespace(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()));
        assertEquals(scaledBrokersCount, knp.getSpec().getReplicas());
        // Node IDs should remain the same
        assertEquals(Constants.REGULAR_BROKER_REPLICAS, knp.getStatus().getNodeIds().size());
        // Kafka should have original Broker Pod count, but in spec there should be the new count
        WaitUtils.waitForKafkaBrokerNodePoolReplicasInSpec(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);
        WaitUtils.waitForPodsReady(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), Constants.REGULAR_BROKER_REPLICAS, true);

        LOGGER.debug("Resume Kafka reconciliation using UI");
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON, "Resume Reconciliation");
        tcc.page().click(CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        // Reconciliation is resumed and button should display Pause
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON, "Pause Reconciliation");

        // Check annotation
        WaitUtils.waitForKafkaAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");

        // Resuming reconciliaiton should trigger scaling
        WaitUtils.waitForPodsReady(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), scaledBrokersCount, true);
    }


    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
