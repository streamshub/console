package io.streamshub.console.api.ui;

import io.streamshub.console.kafka.systemtest.TestPlainProfile;
import io.streamshub.console.kafka.systemtest.utils.ConsumerUtils;
import io.streamshub.console.kafka.systemtest.utils.ConsumerUtils.ConsumerResponse;
import io.streamshub.console.kafka.systemtest.utils.ConsumerUtils.ConsumerType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaGroupsIT extends BaseKafkaIT {

    static final Pattern GROUPS_TITLE   = ci("Groups");
    static final Pattern STABLE_STATE   = ci("stable");
    static final Pattern EMPTY_STATE    = ci("empty");
    static final Pattern RESET_ACTION   = ci("reset");

    ConsumerUtils consumerUtils;

    final String prefix      = "ui-groups-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topicName   = prefix + "topic";
    final String stableGroup = prefix + "stable";
    final String emptyGroup  = prefix + "empty";

    ConsumerResponse<Consumer<String, String>> stableConsumer;

    @BeforeEach
    void setup() {
        consumerUtils = new ConsumerUtils(bootstrapServers, config);
        applyKafka();

        stableConsumer = consumerUtils.request(ConsumerType.CLASSIC)
                .groupId(stableGroup)
                .topic(topicName)
                .messagesPerTopic(1)
                .consume();

        consumerUtils.request(ConsumerType.CLASSIC)
                .groupId(emptyGroup)
                .topic(topicName)
                .createTopic(false)
                .messagesPerTopic(1)
                .autoClose(true)
                .consume();
    }

    @AfterEach
    void cleanup() {
        if (stableConsumer != null) {
            stableConsumer.close(); // must close first: only empty groups can be deleted
        }

        consumerUtils.deleteGroups();
        // The Kafka CR is deleted by BaseKafkaIT.baseCleanup()
    }

    private String groupsUrl() {
        return home + "kafka/" + clusterId + "/groups";
    }

    private static Locator rowFor(Page page, String groupId) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(groupId));
    }

    private Locator resetAction(Page page, String groupId) {
        rowFor(page, groupId)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("kebab|actions")))
                .click();
        return page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(RESET_ACTION));
    }

    @Test
    void pageShowsTitle() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(GROUPS_TITLE))).isVisible();
        }
    }

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            for (String header : new String[] {
                "Group ID", "Type", "Protocol", "State", "lag", "Members", "Topics" }) {
                assertThat(page.getByRole(AriaRole.COLUMNHEADER,
                        new Page.GetByRoleOptions().setName(ci(header))).first()).isVisible();
            }
        }
    }

    @Test
    void tableListsBothGroups() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(rowFor(page, stableGroup)).hasCount(1);
            assertThat(rowFor(page, emptyGroup)).hasCount(1);
        }
    }

    @Test
    void groupStatesMatchBackend() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(rowFor(page, stableGroup)).containsText(STABLE_STATE);
            assertThat(rowFor(page, emptyGroup)).containsText(EMPTY_STATE);
        }
    }

    @Test
    void groupIdLinksToGroupPage() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            Locator link = rowFor(page, stableGroup)
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(stableGroup));
            assertThat(link).hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/groups/.+"));
        }
    }

    @Test
    void groupRowShowsSubscribedTopicAsLinkToTopicPage() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            Locator topicLink = rowFor(page, stableGroup)
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(topicName));
            assertThat(topicLink)
                    .hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/topics/[^/]+$"));
        }
    }

    @Test
    void filteringByGroupIdShowsOnlyMatchingGroup() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(rowFor(page, stableGroup)).hasCount(1);
            assertThat(rowFor(page, emptyGroup)).hasCount(1);

            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();
            filter.fill(stableGroup);
            filter.press("Enter");

            assertThat(rowFor(page, emptyGroup)).hasCount(0);
            assertThat(rowFor(page, stableGroup)).hasCount(1);
        }
    }

    @Test
    void resetOffsetIsEnabledOnlyForEmptyGroups() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());
            assertThat(rowFor(page, emptyGroup)).hasCount(1);

            assertThat(resetAction(page, emptyGroup)).isEnabled();
            page.keyboard().press("Escape");

            assertThat(resetAction(page, stableGroup)).isDisabled();
        }
    }

    @Test
    void resetOffsetOnEmptyGroupOpensModal() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());
            assertThat(rowFor(page, emptyGroup)).hasCount(1);

            resetAction(page, emptyGroup).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).isVisible();
        }
    }
}
