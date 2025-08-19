package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.annotations.SetupSharedResources;
import com.github.streamshub.systemtests.annotations.UseSharedResources;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.interfaces.UseSharedResourcesExtension;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SharedResourcesParameterResolver.class)
public class CustomST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(CustomST.class);
    private static TestCaseConfig tcc;

    private static final String SHARED_1 = "CustomST_shared-1";
    private static final String SHARED_2 = "CustomST_shared2";

    @Test
    void testUnrelated() {
        LOGGER.info("Unrelated test, no topics - setup some do see their deletion after test");
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.topicPrefixName("unrelated"), 1, true, 1, 1, 1);

        LOGGER.info("Unrelated test using topics {}",
            ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName())).size());
    }

    @SetupSharedResources(SHARED_1)
    private Object[] testParams() {
        // Return the values you want to pass to the test
        int count = 10;  // Example
        ResetOffsetType type = ResetOffsetType.EARLIEST;
        ResetOffsetDateTimeType type2 = ResetOffsetDateTimeType.UNIX_EPOCH;
        String index = "0";

        return new Object[]{count, type, type2, index};
    }

    @Test
    @UseSharedResources(SHARED_1)
    void testWithTopics1() {
        LOGGER.info("testWithTopics1 using topics {}",
            ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName())).size());
    }

    @Test
    @UseSharedResources(SHARED_1)
    void testWithTopics2(int count, ResetOffsetType type, ResetOffsetDateTimeType type2, String index) {
        LOGGER.info("testWithTopics2");
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.topicPrefixName("testWithTopics2"), 1, true, 1, 1, 1);

        LOGGER.info("testWithTopics2 using topics {}",
            ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName())).size());
    }

    // // Test Setup
    // @SetupSharedResources(SHARED_1)
    // public void createSharedTopics1(ExtensionContext extensionContext, int count, ResetOffsetType type, ResetOffsetDateTimeType type2, String index) {
    //     KubeResourceManager.get().setTestContext(extensionContext);
    //     LOGGER.info("Creating topics part 2");
    //     KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), "", 2, true, 1, 1, 1);
    //     LOGGER.info("Created topics part2: {}",
    //         ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName() + "-2")).size());
    //     KubeResourceManager.get().printCurrentResources(Level.DEBUG);
    //     KubeResourceManager.get().printAllResources(Level.DEBUG);
    // }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = new TestCaseConfig(KubeResourceManager.get().getTestContext());
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
