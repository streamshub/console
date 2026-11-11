package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;
import jakarta.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaUsersIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;
    static final String NAMESPACE  = "default";
    static final String CLUSTER_LABEL = "strimzi.io/cluster";

    static final String CREATED = "2026-01-15T10:30:00Z";

    static final Pattern USERS_TITLE = ci("users");

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL home;

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    KafkaClusterService kafkaClusterService;

    TestHelper utils;
    URI bootstrapServers;
    String clusterId;

    final String prefix  = "ui-users-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String alpha   = prefix + "alpha";
    final String bravo   = prefix + "bravo";
    final String charlie = prefix + "charlie";
    final String other   = prefix + "other";

    @BeforeEach
    void setup() {
        context.setDefaultTimeout(15_000);
        PlaywrightAssertions.setDefaultAssertionTimeout(15_000);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        deleteStrimziResources();
        applyKafka();
    }

    @AfterEach
    void cleanup() {
        deleteStrimziResources();
    }

   
    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private void deleteStrimziResources() {
        client.resources(KafkaUser.class).inAnyNamespace().delete();
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    private void applyKafka() {
        clusterId = consoleConfig.getKafka().getCluster(KAFKA_KEY).get().getId(); // "k1-id"

        Kafka kafka = utils.apply(client, new KafkaBuilder(
                utils.buildKafkaResource(KAFKA_NAME, clusterId, bootstrapServers))
                .editOrNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus("True")
                    .endCondition()
                .endStatus()
                .build());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> configuredContexts.values().stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::clusterKey)
                        .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));
    }

    private KafkaUser user(String name, String kafkaCluster, KafkaUserAuthentication auth, String username) {
        return new KafkaUserBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(NAMESPACE)
                    .withCreationTimestamp(CREATED)
                    .addToLabels(CLUSTER_LABEL, kafkaCluster)
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(auth)
                .endSpec()
                .withNewStatus()
                    .withUsername(username)
                .endStatus()
                .build();
    }

    private void apply(KafkaUser user) {
        utils.apply(client, user);
    }

    private void createDefaultUsers() {
        apply(user(alpha, KAFKA_NAME, new KafkaUserScramSha512ClientAuthentication(), alpha));
        apply(user(bravo, KAFKA_NAME, new KafkaUserTlsClientAuthentication(), "CN=" + bravo));
        apply(user(charlie, KAFKA_NAME, new KafkaUserScramSha512ClientAuthentication(), charlie));
        apply(user(other, "test-kafka2", new KafkaUserScramSha512ClientAuthentication(), other));

        awaitUsersIndexed(alpha, bravo, charlie);
    }

    private void awaitUsersIndexed(String... names) {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            APIResponse response = context.request().get(home + "api/kafkas/" + clusterId + "/users");
            if (response.status() == 404) {
                return;
            }
            String body = response.text();
            assertTrue(response.ok(), "status " + response.status() + ": " + body);
            for (String name : names) {
                assertTrue(body.contains(name), "missing " + name + " in " + body);
            }
        });
    }

    private String usersUrl() {
        return home + "kafka/" + clusterId + "/users";
    }

    private static Locator rowFor(Page page, String text) {
        return page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(text));
    }

    private static Locator header(Page page, Pattern name) {
        return page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName(name)).first();
    }

    private static Locator cell(Locator row, int index) {
        return row.getByRole(AriaRole.CELL).nth(index);
    }

    /** Names in the order the table currently shows them (first cell of every body row). */
    private static List<String> displayedNames(Page page) {
        return page.locator("tbody tr td:nth-child(1)").allTextContents().stream()
                .map(String::trim)
                .toList();
    }

    @Test
    void pageShowsTitleAndTable() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(USERS_TITLE))).isVisible();
            assertThat(rowFor(page, alpha)).hasCount(1);
        }
    }

    @Test
    void tableShowsExpectedColumns() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());
            assertThat(rowFor(page, alpha)).hasCount(1);

            assertThat(header(page, ci("^name$"))).isVisible();
            assertThat(header(page, ci("^namespace$"))).isVisible();
            assertThat(header(page, ci("creation"))).isVisible();
            assertThat(header(page, ci("^username$"))).isVisible();
            assertThat(header(page, ci("authentication"))).isVisible();
        }
    }

    @Test
    void listsTheUsersOfThisKafkaCluster() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());

            assertThat(rowFor(page, alpha)).hasCount(1);
            assertThat(rowFor(page, bravo)).hasCount(1);
            assertThat(rowFor(page, charlie)).hasCount(1);
        }
    }

    @Test
    void userOfAnotherKafkaClusterIsNotListed() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());
            assertThat(rowFor(page, alpha)).hasCount(1);

            assertThat(rowFor(page, other)).hasCount(0);
        }
    }

    @Test
    void userNameLinksToUserPage() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());

            Locator link = rowFor(page, alpha)
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(alpha));
            assertThat(link).hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/users/.+"));
        }
    }

    @Test
    void clickingUserNameNavigatesToUserPage() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());

            rowFor(page, alpha)
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(alpha))
                    .click();

            assertThat(page).hasURL(Pattern.compile(".*/kafka/" + clusterId + "/users/.+"));
        }
    }

    @Test
    void filteringByNameShowsOnlyMatchingUser() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());
            assertThat(rowFor(page, bravo)).hasCount(1);

            // The only text input on the page is the name filter (it filters on the username)
            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();
            filter.fill(alpha);
            filter.press("Enter");

            assertThat(rowFor(page, alpha)).hasCount(1);
            assertThat(rowFor(page, bravo)).hasCount(0);
            assertThat(rowFor(page, charlie)).hasCount(0);
        }
    }

    @Test
    void filterWithoutMatchShowsNoUsers() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());
            assertThat(rowFor(page, alpha)).hasCount(1);

            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();
            filter.fill("no-such-user-" + prefix);
            filter.press("Enter");

            assertThat(rowFor(page, alpha)).hasCount(0);
            assertThat(rowFor(page, bravo)).hasCount(0);
            assertThat(rowFor(page, charlie)).hasCount(0);
        }
    }

    @Test
    void sortingByNameReordersTheRows() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());
            assertThat(rowFor(page, alpha)).hasCount(1);

            List<String> ascending = List.of(alpha, bravo, charlie);
            List<String> descending = ascending.stream().sorted(Comparator.reverseOrder()).toList();

            Locator nameHeader = header(page, ci("^name$"));

            nameHeader.getByRole(AriaRole.BUTTON).click();
            assertThat(nameHeader).hasAttribute("aria-sort", "ascending");
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertEquals(ascending, displayedNames(page)));

            nameHeader.getByRole(AriaRole.BUTTON).click();
            assertThat(nameHeader).hasAttribute("aria-sort", "descending");
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertEquals(descending, displayedNames(page)));
        }
    }

    @Test
    void onlyTheNameColumnIsSortable() {
        createDefaultUsers();

        try (Page page = context.newPage()) {
            page.navigate(usersUrl());
            assertThat(rowFor(page, alpha)).hasCount(1);

            assertThat(header(page, ci("^name$")).getByRole(AriaRole.BUTTON)).hasCount(1);
            assertThat(header(page, ci("^namespace$")).getByRole(AriaRole.BUTTON)).hasCount(0);
            assertThat(header(page, ci("creation")).getByRole(AriaRole.BUTTON)).hasCount(0);
            assertThat(header(page, ci("^username$")).getByRole(AriaRole.BUTTON)).hasCount(0);
            assertThat(header(page, ci("authentication")).getByRole(AriaRole.BUTTON)).hasCount(0);
        }
    }

    @Test
    void clusterWithoutUsersListsNoUsers() {
        // no KafkaUser resources were created
        try (Page page = context.newPage()) {
            page.navigate(usersUrl());

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(USERS_TITLE))).isVisible();
            assertThat(page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(Pattern.compile(prefix)))).hasCount(0);
            assertThat(rowFor(page, prefix)).hasCount(0);
        }
    }
}
