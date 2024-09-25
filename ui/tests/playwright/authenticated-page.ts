import type { Page } from "@playwright/test";

export class AuthenticatedPage {
  private readonly baseUrl: string;

  constructor(public readonly page: Page) {
    this.baseUrl = process.env.TEST_BASE_URL;
  }

  async goToClusterOverview() {
    await this.page.goto(this.baseUrl);
  }

  async clickLink(text: string, where: "main" | "sidebar" = "main") {
    const link = this.page
      .locator(where === "main" ? "main" : "#page-sidebar")
      .locator("a")
      .locator(`text="${text}"`);
    await link.click();
    const url = await link.getAttribute("href");
    await this.awaitLink(url);
  }

  async awaitLink(url: string) {
    // the first ** is to handle relative links
    // the last ** is to handle redirects, sometimes we link to a page that redirects to another page
    const waitUrl = `${url.startsWith(this.baseUrl) ? "" : "**"}${url.startsWith("/") ? "" : "/"}${url}`;
    try {
      await this.page.waitForURL(waitUrl, { waitUntil: "networkidle" });
    } catch {
      await this.page.waitForURL(waitUrl + "/**", { waitUntil: "networkidle" });
    }
  }

  async goToTopics(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickLink("Topics", "sidebar");
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async goToFirstTopic(waitForLoaded = true) {
    await this.goToTopics(waitForLoaded);
    await this.clickFirstLinkInTheTable("Topics");
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async goToConsumerGroups(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickLink("Consumer groups", "sidebar");
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async waitForTableLoaded() {
    await this.page.waitForSelector('text="Loading data"', {
      state: "hidden",
    });
  }

  async clickFirstLinkInTheTable(tableLabel: string) {
    const link = this.page
      .locator(`table[aria-label="${tableLabel}"]`)
      .locator("tbody")
      .locator("tr")
      .first()
      .locator("td a")
      .first();
    await link.click();
    const url = await link.getAttribute("href");
    await this.awaitLink(url);
  }
}
