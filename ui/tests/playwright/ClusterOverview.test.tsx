import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Cluster Overview page", async ({ page }) => {
  await test.step("Navigate to cluster overview page", async () => {
    await page.click('text="Cluster overview"');
    await page.waitForSelector(
      'text="Key performance indicators and important information regarding the Kafka cluster."',
      { timeout: 500000 },
    );
  });
  await test.step("Cluster overview page should display correctly", async () => {
    const newPage = page.mainFrame();
    const pageMain = newPage.locator("main");

    await expect(pageMain).toContainText("Cluster overview");
    await expect(pageMain).toContainText(
      "Key performance indicators and important information regarding the Kafka cluster.",
    );
    await expect(pageMain).toContainText("Online brokers");
    await expect(pageMain).toContainText("Groups");
    await expect(pageMain).toContainText("Kafka version");
    await expect(pageMain).toContainText("Used disk space");
    await expect(pageMain).toContainText("CPU usage");
    await expect(pageMain).toContainText("Memory usage");
    await expect(pageMain).toContainText("Topic metrics");
    await expect(pageMain).toContainText("Topics bytes incoming and outgoing");
  });
});
