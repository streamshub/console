package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class HomePageIT {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL home;

    @Test
    void listsConfiguredClusters() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            // Verify Kafka clusters configured in config-testplain.yaml
            assertThat(page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("test-kafka1")
            ).first()).isVisible();

            assertThat(page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("test-kafka2")
            ).first()).isVisible();

            assertThat(page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("test-kafka3")
            ).first()).isVisible();
        }
    }

    @Test
    void filterByNameNarrowsTheTable() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            page.getByPlaceholder("Filter by name")
                    .fill("test-kafka2");

            page.keyboard().press("Enter");

            // test-kafka1 should not be displayed
            assertThat(page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("test-kafka1")
            )).hasCount(0);

            // test-kafka2 should be displayed
            assertThat(page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("test-kafka2")
            ).first()).isVisible();
        }
    }

    @Test
    void viewButtonNavigatesToCluster() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("test-kafka1")
            ).first().click();

            assertThat(page).hasURL("**/kafka/*/overview");
        }
    }
}
