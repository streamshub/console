
package io.streamshub.console.api.ui;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import io.streamshub.console.kafka.systemtest.TestPlainProfile;
import io.streamshub.console.test.TopicHelper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaTopicsIT extends BaseKafkaIT {
    static final Pattern TOPICS_TITLE = ci("Topics");
    static final Pattern NAME_COLUMN = ci("Name");
    static final Pattern STATUS_COLUMN = ci("Status");
    static final Pattern PARTITIONS_COL = ci("Partitions");
    static final Pattern GROUPS_COLUMN = ci("Group");
    static final Pattern STORAGE_COLUMN = ci("Storage");
    static final Pattern MANAGED_LABEL = ci("Managed");

    static final Pattern TOTAL_LABEL =
            Pattern.compile("\\d+[\\s\\u00A0]*total");

    static final String LISTING_FIELDS =
            "name,status,numPartitions,totalLeaderLogBytes,groups";

    @TestHTTPResource("/")
    URL home;

    TopicHelper topicHelper;

    final String prefix =
            "ui-topics-" + UUID.randomUUID().toString().substring(0, 8) + "-";

    final String ordersTopic = prefix + "orders";
    final String paymentsTopic = prefix + "payments";
    final String eventsTopic = prefix + "events";

    @BeforeEach
    void topicsSetup() {
        topicHelper = new TopicHelper(bootstrapServers, config);

        applyKafka();

        createTopics();
    }

    @AfterEach
    void topicsCleanup() {
        if (topicHelper != null) {
            topicHelper.deleteTopics(
                    ordersTopic,
                    paymentsTopic,
                    eventsTopic);
        }
    }

    private void createTopics() {
        topicHelper.createTopics(List.of(ordersTopic), 3);
        topicHelper.createTopics(List.of(paymentsTopic), 1);
        topicHelper.createTopics(List.of(eventsTopic), 2);
    }

    private String topicsUrl() {
        return home + "kafka/" + clusterId + "/topics";
    }

    private static Locator rowFor(Page page, String topicName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions()
                        .setHasText(topicName));
    }

    @Test
    void headerShowsTitleAndTotal() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setLevel(1)
                            .setName(TOPICS_TITLE)))
                    .isVisible();

            assertThat(page.getByText(TOTAL_LABEL).first())
                    .isVisible();
        }
    }

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            for (Pattern header : List.of(
                    NAME_COLUMN,
                    STATUS_COLUMN,
                    PARTITIONS_COL,
                    GROUPS_COLUMN,
                    STORAGE_COLUMN)) {

                assertThat(page.getByRole(
                        AriaRole.COLUMNHEADER,
                        new Page.GetByRoleOptions()
                                .setName(header))
                        .first())
                        .isVisible();
            }
        }
    }

    @Test
    void tableListsCreatedTopics() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, ordersTopic)).hasCount(1);
            assertThat(rowFor(page, paymentsTopic)).hasCount(1);
            assertThat(rowFor(page, eventsTopic)).hasCount(1);
        }
    }

    @Test
    void topicRowLinksPointToTopicPages() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            Locator row = rowFor(page, ordersTopic);

            assertThat(row).hasCount(1);
            assertThat(row.getByRole(
                    AriaRole.LINK,
                    new Locator.GetByRoleOptions()
                            .setName(ordersTopic)))
                    .hasAttribute(
                            "href",
                            Pattern.compile(
                                    ".*/kafka/"
                                    + clusterId
                                    + "/topics/[^/]+$"));

            assertThat(row.getByRole(
                    AriaRole.LINK,
                    new Locator.GetByRoleOptions()
                            .setName("3")
                            .setExact(true)))
                    .hasAttribute(
                            "href",
                            Pattern.compile(
                                    ".*/topics/[^/]+/partitions$"));

            assertThat(row.getByRole(
                    AriaRole.LINK,
                    new Locator.GetByRoleOptions()
                            .setName("0")
                            .setExact(true)))
                    .hasAttribute(
                            "href",
                            Pattern.compile(
                                    ".*/topics/[^/]+/groups$"));
        }
    }

    @Test
    void partitionCountsMatchCreatedTopics() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, paymentsTopic)
                    .getByRole(
                            AriaRole.LINK,
                            new Locator.GetByRoleOptions()
                                    .setName("1")
                                    .setExact(true)))
                    .hasCount(1);

            assertThat(rowFor(page, eventsTopic)
                    .getByRole(
                            AriaRole.LINK,
                            new Locator.GetByRoleOptions()
                                    .setName("2")
                                    .setExact(true)))
                    .hasCount(1);
        }
    }

    @Test
    void unmanagedTopicsHaveNoManagedLabel() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, ordersTopic)).hasCount(1);

            assertThat(rowFor(page, ordersTopic))
                    .not()
                    .containsText(MANAGED_LABEL);
        }
    }

    @Test
    void listRequestAsksForListingFields() {
        try (Page page = context.newPage()) {
            List<String> requests = new CopyOnWriteArrayList<>();

            page.onRequest(request ->
                    requests.add(URLDecoder.decode(
                            request.url(),
                            StandardCharsets.UTF_8)));

            page.navigate(topicsUrl());

            assertThat(rowFor(page, ordersTopic)).hasCount(1);

            assertTrue(
                    requests.stream().anyMatch(url ->
                            url.contains(
                                    "/api/kafkas/"
                                    + clusterId
                                    + "/topics")
                            && url.contains(LISTING_FIELDS)),
                    "Expected a topics request with fields="
                            + LISTING_FIELDS
                            + " but got "
                            + requests);
        }
    }

    @Test
    void filteringByNameShowsOnlyMatchingTopics() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, paymentsTopic)).hasCount(1);
            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();

            filter.fill(ordersTopic);
            filter.press("Enter");

            assertThat(rowFor(page, ordersTopic)).hasCount(1);
            assertThat(rowFor(page, paymentsTopic)).hasCount(0);
            assertThat(rowFor(page, eventsTopic)).hasCount(0);
        }
    }
}

