package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testchecks.KafkaNodePoolChecks;
import com.github.streamshub.systemtests.utils.testutils.KafkaTestUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBroker;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

public class KafkaNodePoolST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaST.class);
    private static final String FILTER_RUNNING_KNP_BUCKET = "FilterRunningKnp";
    protected TestCaseConfig tcc;

    private static final String ADDITIONAL_BRK_KNP_NAME = "additional-brk";
    private static final int ADDITIONAL_BRK_NODES = 2;

    /**
     * Verifies filtering of Kafka Node Pools (KNP) by type and name in the UI.
     *
     * <p>This test runs under the {@code FILTER_RUNNING_KNP_BUCKET} and validates
     * that both default and additional Kafka Node Pools (broker and controller)
     * are correctly displayed and filtered in the Console.</p>
     *
     * <p>The test performs the following steps:</p>
     * <ul>
     *     <li>Retrieves broker and controller node IDs from:
     *         <ul>
     *             <li>Default broker and controller node pools</li>
     *             <li>Additional broker node pools</li>
     *         </ul>
     *     </li>
     *     <li>Verifies the default node state contains all expected brokers and controllers.</li>
     *     <li>Filters node pools by name and verifies:
     *         <ul>
     *             <li>Default broker pool displays only broker nodes</li>
     *             <li>Additional broker pool displays only broker nodes</li>
     *             <li>Default controller pool displays only controller nodes</li>
     *         </ul>
     *     </li>
     *     <li>Resets filters after each validation and verifies the total node count is restored.</li>
     * </ul>
     */
    @TestBucket(FILTER_RUNNING_KNP_BUCKET)
    @Test
    void testFilterKafkaNodesByType() {
        LOGGER.debug("Fetching default broker and controller node IDs");
        List<Integer> defaultBrokerIds = KafkaUtils.getKnpIds(tcc.namespace(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()), ProcessRoles.BROKER);
        List<Integer> defaultControllerIds = KafkaUtils.getKnpIds(tcc.namespace(), KafkaNamingUtils.controllerPoolName(tcc.kafkaName()), ProcessRoles.CONTROLLER);

        LOGGER.debug("Fetching additional broker node IDs");
        List<Integer> addedBrokerIds = KafkaUtils.getKnpIds(tcc.namespace(), ADDITIONAL_BRK_KNP_NAME, ProcessRoles.BROKER);
        List<Integer> brokerIds = Stream.of(defaultBrokerIds, addedBrokerIds).flatMap(List::stream).toList();
        int totalNodeCount = brokerIds.size() + defaultControllerIds.size();

        KafkaNodePoolChecks.checkDefaultNodeState(tcc, brokerIds, defaultControllerIds);

        // Filter brokers
        LOGGER.info("Filtering default broker node pool: {}", KafkaNamingUtils.brokerPoolName(tcc.kafkaName()));
        KafkaTestUtils.filterKnpByName(tcc, KafkaNamingUtils.brokerPoolName(tcc.kafkaName()));
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, defaultBrokerIds, ProcessRoles.BROKER.toValue(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()));
        KafkaTestUtils.resetKnpFilters(tcc, totalNodeCount);

        LOGGER.info("Filtering additional broker node pool: {}", ADDITIONAL_BRK_KNP_NAME);
        KafkaTestUtils.filterKnpByName(tcc, ADDITIONAL_BRK_KNP_NAME);
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, addedBrokerIds, ProcessRoles.BROKER.toValue(), ADDITIONAL_BRK_KNP_NAME);
        KafkaTestUtils.resetKnpFilters(tcc, totalNodeCount);

        // Filter controllers
        LOGGER.info("Filtering default controller node pool: {}", KafkaNamingUtils.controllerPoolName(tcc.kafkaName()));
        KafkaTestUtils.filterKnpByName(tcc, KafkaNamingUtils.controllerPoolName(tcc.kafkaName()));
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, defaultControllerIds, ProcessRoles.CONTROLLER.toValue(), KafkaNamingUtils.controllerPoolName(tcc.kafkaName()));
        KafkaTestUtils.resetKnpFilters(tcc, totalNodeCount);
    }

    /**
     * Verifies filtering of Kafka Nodes by role (Broker or Controller) in the UI.
     *
     * <p>This test runs under the {@code FILTER_RUNNING_KNP_BUCKET} and ensures that
     * role-based filtering correctly displays Kafka nodes across both default and
     * additional Kafka Node Pools.</p>
     *
     * <p>The test performs the following steps:</p>
     * <ul>
     *     <li>Retrieves broker and controller node IDs from:
     *         <ul>
     *             <li>Default broker and controller node pools</li>
     *             <li>Additional broker node pools</li>
     *         </ul>
     *     </li>
     *     <li>Combines all broker IDs to determine the total node count.</li>
     *     <li>Verifies the default UI state contains all expected nodes.</li>
     *     <li>Applies role-based filtering for:
     *         <ul>
     *             <li>{@code Broker} role – verifies only broker nodes are displayed.</li>
     *             <li>{@code Controller} role – verifies only controller nodes are displayed.</li>
     *         </ul>
     *     </li>
     *     <li>Resets filters after each validation and confirms the total node count is restored.</li>
     * </ul>
     */
    @TestBucket(FILTER_RUNNING_KNP_BUCKET)
    @Test
    void testFilterKafkaNodesByRole() {
        LOGGER.debug("Fetching default broker and controller node IDs");
        List<Integer> defaultBrokerIds = KafkaUtils.getKnpIds(tcc.namespace(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()), ProcessRoles.BROKER);
        List<Integer> defaultControllerIds = KafkaUtils.getKnpIds(tcc.namespace(), KafkaNamingUtils.controllerPoolName(tcc.kafkaName()), ProcessRoles.CONTROLLER);

        LOGGER.debug("Fetching additional broker IDs");
        List<Integer> addedBrokerIds = KafkaUtils.getKnpIds(tcc.namespace(), ADDITIONAL_BRK_KNP_NAME, ProcessRoles.BROKER);
        List<Integer> brokerIds = Stream.of(defaultBrokerIds, addedBrokerIds).flatMap(List::stream).toList();
        int totalNodeCount = brokerIds.size() + defaultControllerIds.size();

        KafkaNodePoolChecks.checkDefaultNodeState(tcc, brokerIds, defaultControllerIds);

        // Brokers
        LOGGER.info("Filtering Kafka nodes by role: {}", ProcessRoles.BROKER.toValue());
        KafkaTestUtils.filterKnpByRole(tcc, ProcessRoles.BROKER.toValue());

        LOGGER.debug("Validating that only broker nodes are displayed");
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, brokerIds, ProcessRoles.BROKER.toValue(), null);
        KafkaTestUtils.resetKnpFilters(tcc, totalNodeCount);

        // Controllers
        LOGGER.info("Filtering Kafka nodes by role: {}", ProcessRoles.CONTROLLER.toValue());
        KafkaTestUtils.filterKnpByRole(tcc, ProcessRoles.CONTROLLER.toValue());

        LOGGER.debug("Validating that only controller nodes are displayed");
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, defaultControllerIds, ProcessRoles.CONTROLLER.toValue(), null);
        KafkaTestUtils.resetKnpFilters(tcc, totalNodeCount);
    }

    @SetupTestBucket(FILTER_RUNNING_KNP_BUCKET)
    public void prepareFilterKafkaNodePools() {
        // Add additional KNP for filtering, due to quorum voters it's currently possible to add only broker role node pools
        // controller node pools cause crash
        // -> Configuration can't be updated dynamically because its scope is ready only: AlterConfigOp(name=controller.quorum.voters)
        KafkaNodePool addedBrokerPool = KafkaSetup.getDefaultBrokerNodePools(tcc.namespace(), tcc.kafkaName(), ADDITIONAL_BRK_NODES)
            .editMetadata()
                .withName(ADDITIONAL_BRK_KNP_NAME)
            .endMetadata()
            .build();
        KubeResourceManager.get().createOrUpdateResourceWithWait(addedBrokerPool);
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpLabelSelector(tcc.kafkaName(), ADDITIONAL_BRK_KNP_NAME, ProcessRoles.BROKER), ADDITIONAL_BRK_NODES, true);

        // Update kafka to accept new brokers
        List<GenericKafkaListenerConfigurationBroker> brokerHosts = KafkaUtils.getKnpIds(tcc.namespace(), ADDITIONAL_BRK_KNP_NAME, ProcessRoles.BROKER)
            .stream()
            .sorted()
            .map(id -> new GenericKafkaListenerConfigurationBrokerBuilder()
                .withBroker(id)
                .withHost(String.join(".", "broker-" + id, Utils.hashStub(tcc.namespace()), tcc.kafkaName(), ClusterUtils.getClusterDomain()))
                .build())
            .toList();

        KubeResourceManager.get().updateResource(
            new KafkaBuilder(ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName()))
                .editSpec()
                    .editKafka()
                        .editMatchingListener(l -> l.getName().equals(Constants.SECURE_LISTENER_NAME))
                            .editConfiguration()
                                .addToBrokers(brokerHosts.toArray(new GenericKafkaListenerConfigurationBroker[0]))
                            .endConfiguration()
                        .endListener()
                    .endKafka()
                .endSpec()
                .build());

        WaitUtils.waitForKafkaReady(tcc.namespace(), tcc.kafkaName());
    }

    @BeforeAll
    void testClassSetup() {
        tcc = Utils.getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }

}
