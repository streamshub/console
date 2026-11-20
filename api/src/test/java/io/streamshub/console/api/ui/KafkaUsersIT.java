package io.streamshub.console.api.ui;

import io.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaUsersIT extends BaseKafkaIT {
    static final String NAMESPACE  = "default";
    static final String CLUSTER_LABEL = "strimzi.io/cluster";

    static final String CREATED = "2026-01-15T10:30:00Z";

    static final Pattern USERS_TITLE = ci("users");

    @TestHTTPResource("/")
    URL home;

    final String prefix  = "ui-users-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String alpha   = prefix + "alpha";
    final String bravo   = prefix + "bravo";
    final String charlie = prefix + "charlie";
    final String other   = prefix + "other";

    @BeforeEach
    void usersSetup() {
        applyKafka();
    }

    @Override
    protected void deleteStrimziResources() {
        client.resources(KafkaUser.class).inAnyNamespace().delete();
        super.deleteStrimziResources();
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
        await().atMost(15, TimeUnit.SECONDS)
                .pollInSameThread()
                .untilAsserted(() -> {
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

    private static Locator displayedNames(Page page) {
        return page.locator("tbody tr td:nth-child(1)");
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
            Locator names = displayedNames(page);

            nameHeader.getByRole(AriaRole.BUTTON).click();
            assertThat(nameHeader).hasAttribute("aria-sort", "ascending");
            assertThat(names).hasText(ascending.toArray(new String[0]));

            nameHeader.getByRole(AriaRole.BUTTON).click();
            assertThat(nameHeader).hasAttribute("aria-sort", "descending");
            assertThat(names).hasText(descending.toArray(new String[0]));
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
