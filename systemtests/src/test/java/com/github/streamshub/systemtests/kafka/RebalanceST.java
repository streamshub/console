package com.github.streamshub.systemtests.kafka;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.ConsoleLocators;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;

import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceState;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static com.github.streamshub.systemtests.utils.Utils.runAsyncWithContext;

@Tag(TestTags.REGRESSION)
class RebalanceST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(RebalanceST.class);

    /**
     * Tests the full lifecycle of a Kafka rebalance, from proposal generation to
     * manual approval through the UI.
     *
     * <p>The test first creates an imbalance by creating 5 topics (prefix {@code rebalance-topic})
     * with 20 partitions each, replication factor 1, and then scaling the Kafka brokers up to
     * 5 replicas. A {@code KafkaRebalance} custom resource ({@code testrebalance}) with a default
     * (full) rebalance mode is then created and awaited until it reaches the
     * {@code ProposalReady} state.</p>
     *
     * <p>The test then verifies the following through the UI:</p>
     * <ul>
     *   <li>The rebalance proposals table on the Kafka Rebalance page shows a single row with
     *       "Proposal Ready" status and the correct rebalance name.</li>
     *   <li>Expanding the proposal dropdown shows auto-approval disabled ({@code false}) and the
     *       rebalance mode set to {@code full}.</li>
     *   <li>Opening the proposal's data modal displays values (data to move, monitored partitions
     *       percentage, number of replica movements, and balancedness scores before/after) matching
     *       the {@code optimizationResult} reported in the {@code KafkaRebalance} status.</li>
     *   <li>Approving the proposal through the UI (action dropdown &gt; approve &gt; confirm) transitions
     *       the {@code KafkaRebalance} resource to the {@code Rebalancing} state, which is reflected
     *       both in the resource status and in the UI.</li>
     * </ul>
     *
     * <p>This ensures that rebalance proposals are accurately surfaced in the UI and that
     * approving a rebalance from the UI correctly triggers the underlying rebalance operation.</p>
     */
    @Test
    void testKafkaRebalance() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int imbalancedPartitions = 20;
        final String rebalanceName = "testrebalance";
        final String rebalanceTopicName = "rebalance-topic";

        LOGGER.info("Creating imbalance on Kafka '{}' by creating 5 topics (prefix '{}') with {} partitions and replication factor 1", tcc.kafkaName(), rebalanceTopicName, imbalancedPartitions);

        try (Admin admin = KafkaClientsUtils.createSecureClient(tcc, Admin::create)) {
            // every partition assigned to broker node 0
            var replicaAssignments = IntStream.range(0, imbalancedPartitions)
                    .mapToObj(i -> Map.entry(i, List.of(0)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            var topicNames = IntStream.range(0, 5)
                    .mapToObj(String::valueOf).map((rebalanceTopicName + "-")::concat)
                    .toList();

            try {
                // Remove existing topics of the same name, if present.
                var duplicates = admin.listTopics().names().toCompletionStage().toCompletableFuture().join()
                        .stream()
                        .filter(topicNames::contains)
                        .toList();
                if (!duplicates.isEmpty()) {
                    admin.deleteTopics(duplicates).all().toCompletionStage().toCompletableFuture().join();
                }
            } catch (Exception e) {
                // Ignore errors and try to keep going
            }

            var topics = topicNames.stream()
                    .map(name -> new NewTopic(name, replicaAssignments))
                    .toList();

            admin.createTopics(topics).all().toCompletionStage().toCompletableFuture().join();
        }

        LOGGER.info("Creating KafkaRebalance resource '{}' with default (full) mode for Kafka '{}' in namespace '{}'", rebalanceName, tcc.kafkaName(), tcc.namespace());
        KubeResourceManager.get().createOrUpdateResourceWithWait(KafkaSetup.getKafkaRebalance(tcc.namespace(), tcc.kafkaName(), rebalanceName).build());
        LOGGER.info("Waiting for KafkaRebalance '{}' to reach '{}' state", rebalanceName, KafkaRebalanceState.ProposalReady);
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.ProposalReady);

        var consoleLocators = ConsoleLocators.of(tcc.page());
        var rebalancesPage = consoleLocators.nodesRebalance();

        LOGGER.info("Verifying rebalance proposals table shows a single 'Proposal Ready' entry for rebalance '{}'", rebalanceName);
        PwUtils.navigate(tcc, PwPageUrls.getKafkaRebalancePage(tcc, tcc.kafkaName()));
        // control rows contain the primary data (not the expanded content)
        var rebalanceTableControlRows = rebalancesPage.dataView().table().body().controlRows();
        PwUtils.waitForLocatorCount(tcc, 1, rebalanceTableControlRows.locator(), false);
        PwUtils.waitForContainsText(tcc, rebalanceTableControlRows.nth(0).cell("Name"), rebalanceName, false);
        PwUtils.waitForContainsText(tcc, rebalanceTableControlRows.nth(0).cell("Status"), "Proposal Ready", false);

        LOGGER.info("Inspecting rebalance proposal dropdown for auto-approval flag and rebalance mode");
        PwUtils.waitForLocatorAndClick(rebalanceTableControlRows.nth(0).expansionToggle());
        var row0ExpandableContentLocator = rebalancesPage.dataView().table().body().expandableRow(0).cells().first();
        PwUtils.waitForContainsText(tcc, row0ExpandableContentLocator.locator("dt:has-text('Auto-approval enabled') + dd"), "false", false);
        PwUtils.waitForContainsText(tcc, row0ExpandableContentLocator.locator("dt:has-text('Mode') + dd"), "Full", false);

        LOGGER.info("Navigating to rebalance detail page for rebalance '{}' to verify optimization result values", rebalanceName);
        PwUtils.waitForLocatorAndClick(rebalanceTableControlRows.nth(0).cell("Name").locator("a"));

        var rebalanceDetailPage = consoleLocators.rebalanceDetail();
        PwUtils.waitForContainsText(tcc, rebalanceDetailPage.title(), rebalanceName, false);

        // table values
        Map<String, Object> status = ResourceUtils.getKubeResource(KafkaRebalance.class, tcc.namespace(), rebalanceName).getStatus().getOptimizationResult();
        LOGGER.debug("KafkaRebalance '{}' optimizationResult from status: {}", rebalanceName, status);
        var proposalDetail = rebalanceDetailPage.proposalDetail();
        LOGGER.debug("Expanding proposal detail section");
        PwUtils.waitForLocatorAndClick(proposalDetail.expansionToggle());

        // check some values in cards
        for (var property : List.of("numReplicaMovements", "dataToMoveMB")) {
            PwUtils.waitForContainsText(tcc, proposalDetail.cardAttribute(property), status.get(property).toString(), false);
        }

        // check some values in listing
        for (var property : List.of("monitoredPartitionsPercentage", "onDemandBalancednessScoreBefore", "onDemandBalancednessScoreAfter")) {
            PwUtils.waitForContainsText(tcc, proposalDetail.listAttribute(property).description(), status.get(property).toString(), false);
        }

        // navigate back to the listing
        tcc.page().goBack();

        LOGGER.info("Approving rebalance proposal '{}' via UI action dropdown", rebalanceName);
        PwUtils.waitForLocatorAndClick(rebalanceTableControlRows.nth(0).menuToggle());
        PwUtils.waitForLocatorAndClick(rebalancesPage.actionMenu().approveButton());
        PwUtils.waitForLocatorAndClick(rebalancesPage.confirmationModal().confirm());

        LOGGER.info("Verifying that UI approval transitioned KafkaRebalance '{}' to '{}' state", rebalanceName, KafkaRebalanceState.Rebalancing);
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.Rebalancing);
        PwUtils.waitForLocatorAndClick(consoleLocators.globalDataRefresh());

        LOGGER.debug("Confirming '{}' state is reflected in the UI proposal status for rebalance '{}'", KafkaRebalanceState.Rebalancing, rebalanceName);
        PwUtils.waitForContainsText(tcc, rebalanceTableControlRows.nth(0).cell("Status"), KafkaRebalanceState.Rebalancing.name(), false);
    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());

        CompletableFuture.allOf(
                runAsyncWithContext(() -> KafkaSetup.setupKafkaWithCcIfNeeded(tcc.namespace(), tcc.kafkaName())),
                runAsyncWithContext(() -> 
                        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(
                                tcc.namespace(),
                                tcc.consoleInstanceName(),
                                tcc.kafkaName(), 
                                tcc.kafkaUserName())
                            .build()))
            )
            .join();

        PwUtils.login(tcc);
    }
}
