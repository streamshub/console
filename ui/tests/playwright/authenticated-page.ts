import { expect, type Page } from "@playwright/test";

export class AuthenticatedPage {
  private readonly baseUrl: string;

  constructor(public readonly page: Page) {
    this.baseUrl = process.env.TEST_BASE_URL!;
  }

  async goToClusterOverview() {
    await this.page.goto(this.baseUrl, {
      waitUntil: "domcontentloaded",
    });
  }

  async clickNav(name: string) {
    this.page
      .locator("#page-sidebar")
      .getByRole("link", { name: name })
      .click();
  }

  async clickLink(text: string, where: "main" | "sidebar" = "main") {
    const link = this.page
      .locator(where === "main" ? "main" : "#page-sidebar")
      .locator("a")
      .locator(`text="${text}"`);
    link.click();
  }

  async clickTab(text: string, where: "main" | "sidebar" = "main") {
    const tabsContainer = this.page
      .locator(where === "main" ? "main" : "#page-sidebar")
      .locator('[role="tablist"]');
    const tab = tabsContainer.getByRole("tab", { name: text });
    await expect(tab).toBeVisible({ timeout: 5000 });
    await tab.click({ force: true });
    await expect(tab).toHaveAttribute("aria-selected", "true", {
      timeout: 10000,
    });
  }

  async goToTopics(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickNav("Topics");
    await expect(this.page.locator("h1").getByText("Topics")).toBeVisible();
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async goToFirstTopic(waitForLoaded = true) {
    await this.goToTopics(waitForLoaded);
    await this.clickFirstLinkInTheTable("Topics");
    if (waitForLoaded) {
      // wait for 'Topics' link to be in the breadcrumbs
      await expect(
        this.page
          .getByLabel("Breadcrumb")
          .getByRole("link", { name: "Topics" }),
      ).toBeVisible();
      await this.waitForTableLoaded();
    }
  }

  async goToConsumerGroups(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickNav("Groups");
    await expect(
      this.page.locator("h1").getByText("Groups"),
    ).toBeVisible();
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async goToKafkaUser(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickNav("Kafka Users");
    await expect(
      this.page.locator("h1").getByText("Kafka Users"),
    ).toBeVisible();
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async waitForTableLoaded() {
    await expect(
      this.page
        .locator("div")
        .filter({ hasText: /^Loading data$/ })
        .first(),
    ).toBeHidden();
  }

  async clickFirstLinkInTheTable(tableLabel: string) {
    const link = this.page
      .locator(`table[aria-label="${tableLabel}"]`)
      .locator("tbody")
      .locator("tr")
      .first()
      .locator("td a")
      .first();
    link.click();
  }
}
