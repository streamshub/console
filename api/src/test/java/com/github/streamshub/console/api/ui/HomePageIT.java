package com.github.streamshub.console.api.ui;

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
@TestProfile(UITestProfile.class)
class HomePageUITest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL home;

    @Test
    void listsConfiguredClusters() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("kafka1")).first()).isVisible();
            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("kafka2")).first()).isVisible();
            // count text comes from i18n 'kafka.connectedClusters'
            assertThat(page.getByText("2 ", new Page.GetByTextOptions())).isVisible();
        }
    }

    @Test
    void filterByNameNarrowsTheTable() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            page.getByPlaceholder("Filter by name").fill("kafka2"); // match your i18n text
            page.keyboard().press("Enter");

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("kafka1"))).hasCount(0);
            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("kafka2")).first()).isVisible();
        }
    }

    @Test
    void viewButtonNavigatesToCluster() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("kafka1"))
                .first().click();

            page.waitForURL("**/kafka/*");
        }
    }
}
