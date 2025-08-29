package com.github.streamshub.systemtests.consumers;

import com.github.streamshub.console.support.Identifiers;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.SingleConsumerGroupPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.microsoft.playwright.Locator;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils.defaultTopic;

@Tag(TestTags.REGRESSION)
public class ConsumerST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerST.class);
    protected TestCaseConfig tcc;

    public Stream<Arguments> variableConsumerGroupNamesScenario() {
        final int messageCount = Constants.MESSAGE_COUNT;
        return Stream.of(
            Arguments.of("Special chars", messageCount, "group$$$$$%^^&*"),
            Arguments.of("Semicolon separated", messageCount, "group;part;;1"),
            Arguments.of("Dot separated", messageCount, "group.1.3.5"),
            Arguments.of("Colon separated", messageCount, "group:12::3:"),
            Arguments.of("Symbols", messageCount, "group'@!\"#?§±"),
            Arguments.of("Underscores", messageCount, "group_with__underscores_"),
            Arguments.of("Hyphenated", messageCount, "group-hyphen--name-"),
            Arguments.of("With slash", messageCount, "group/with//slash/"),
            Arguments.of("Equals sign", messageCount, "group=equals==two"),
            Arguments.of("Comma separated", messageCount, "group,comma,separated,,"),
            Arguments.of("With spaces", messageCount, "group space allowed"),
            Arguments.of("Pipe symbol", messageCount, "group|pipe||secondpipe"),
            Arguments.of("Tilde", messageCount, "group~tilde~~name"),
            Arguments.of("Unicode checkmark", messageCount, "group©©unicode✓✓test"),
            Arguments.of("Japanese text", messageCount, "group日本のキャラクター"),
            Arguments.of("Czech text", messageCount, "skupina+ěščřžýáíé"),
            Arguments.of("Fancy unicode", messageCount, "group_𝕬𝖑𝖑𝖙𝖊𝖘𝖙𝖘"),
            Arguments.of("Whitespaces regex", messageCount, "\s\s\s"),
            Arguments.of("Very long name", messageCount,
                "consumer_group_with_really_really_really_long_name_1234567890-1234567890-1234567890")
        );
    }

    @ParameterizedTest(name = "Scenario: {0} - ConsumerGroupName: [{2}]")
    @MethodSource("variableConsumerGroupNamesScenario")
    void testVariableConsumerGroupNames(String displayName, int messageCount, String consumerGroupName) {

        String topicName = "topic-" + Utils.hashStub(displayName);
        String consumerGroupEncodedName = Identifiers.encode(consumerGroupName);

        // Must be done due to
        // https://jellepelgrims.com/posts/dollar_signs
        String k8sFriendlyName = consumerGroupName.replace("$", "$$");


        LOGGER.info("Create KafkaTopic CR");
        KubeResourceManager.get().createResourceWithWait(
            defaultTopic(tcc.namespace(), tcc.kafkaName(), topicName, 1, 1, 1).build());

        LOGGER.info("Produce and consume messages");
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(messageCount)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(k8sFriendlyName)
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        LOGGER.info("Verify consumer group is displaying correctly");

        LOGGER.info("Navigate to single consumer group page encoded to '{}'", consumerGroupEncodedName);
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupEncodedName));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, consumerGroupName, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(CssSelectors.PAGES_HEADER_BREADCRUMB_ITEMS).nth(4).build(), consumerGroupName, true);

        LOGGER.info("Navigate to consumer groups page to check group is present");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), ""), PwUtils.getDefaultNavigateOpts());
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), ""), PwUtils.getDefaultNavigateOpts());

        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_HEADER_TITLE, "Consumer Groups", true);
        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS, consumerGroupName, true);

        LOGGER.info("Try click-through link and get redirected to a correct page");
        PwUtils.waitForLocatorAndClick(tcc.page()
            .locator(ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS)
            .locator("a", new Locator.LocatorOptions().setHasText(consumerGroupName)));
        tcc.page().waitForURL(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupEncodedName), PwUtils.getDefaultWaitForUrlOpts());
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, consumerGroupName, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(CssSelectors.PAGES_HEADER_BREADCRUMB_ITEMS).nth(4).build(), consumerGroupName, true);

        LOGGER.info("Check topic page if consumer group is present");
        final String topicId = ResourceUtils.getKubeResource(KafkaTopic.class, tcc.namespace(), topicName).getStatus().getTopicId();
        tcc.page().navigate(PwPageUrls.getSingleTopicConsumerGroupsPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultNavigateOpts());
        tcc.page().waitForURL(PwPageUrls.getSingleTopicConsumerGroupsPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultWaitForUrlOpts());

        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS, consumerGroupName, true);

        LOGGER.info("Try click-through link and get redirected to a correct page");
        PwUtils.waitForLocatorAndClick(tcc.page()
            .locator(ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS)
            .locator("a", new Locator.LocatorOptions().setHasText(consumerGroupName)));
        tcc.page().waitForURL(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupEncodedName), PwUtils.getDefaultWaitForUrlOpts());
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, consumerGroupName, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(CssSelectors.PAGES_HEADER_BREADCRUMB_ITEMS).nth(4).build(), consumerGroupName, true);
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
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
