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
    expect(await newPage.innerText("body")).toContain("Cluster overview");
    expect(await newPage.innerText("body")).toContain(
      "Key performance indicators and important information regarding the Kafka cluster.",
    );
    expect(await newPage.innerText("body")).toContain("Online brokers");
    expect(await newPage.innerText("body")).toContain("Groups");
    expect(await newPage.innerText("body")).toContain("Kafka version");
    expect(await newPage.innerText("body")).toContain("Used disk space");
    expect(await newPage.innerText("body")).toContain("CPU usage");
    expect(await newPage.innerText("body")).toContain("Memory usage");
    expect(await newPage.innerText("body")).toContain("Topic metrics");
    expect(await newPage.innerText("body")).toContain(
      "Topics bytes incoming and outgoing",
    );
  });
});
