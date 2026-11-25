package io.streamshub.console.api.ui;

import io.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class HomePageIT extends BaseKafkaIT {

    private static Locator clusterButton(Page page, String clusterName) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(clusterName));
    }

    @Test
    void listsConfiguredClusters() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            assertThat(clusterButton(page, "test-kafka1").first()).isVisible();
            assertThat(clusterButton(page, "test-kafka2").first()).isVisible();
            assertThat(clusterButton(page, "test-kafkaY").first()).isVisible();
        }
    }

    @Test
    void filterByNameNarrowsTheTable() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            assertThat(clusterButton(page, "test-kafka1").first()).isVisible();
            assertThat(clusterButton(page, "test-kafka2").first()).isVisible();

            Locator filter = page.getByPlaceholder("Filter by name");
            filter.fill("test-kafka2");
            filter.press("Enter");

            // test-kafka1 should not be displayed
            assertThat(clusterButton(page, "test-kafka1")).hasCount(0);

            // test-kafka2 should be displayed
            assertThat(clusterButton(page, "test-kafka2").first()).isVisible();
        }
    }

    @Test
    void clusterNameButtonNavigatesToOverview() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            clusterButton(page, "test-kafka1").first().click();

            assertThat(page).hasURL(Pattern.compile(".*/kafka/[^/]+/overview/?$"));
        }
    }
}
