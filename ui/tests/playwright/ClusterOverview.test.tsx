import { expect, test } from "@playwright/test";

test.describe("Cluster Overview page", () => {
  test("Cluster overview page should display correctly", async ({ page }) => {
    await page.goto(`./kafka/j7W3TRG7SsWCBXHjz2hfrg/overview`);
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Cluster overview");
    expect(await page.innerText("body")).toContain(
      "Key performance indicators and important information regarding the Kafka cluster.",
    );
    expect(await page.innerText("body")).toContain("Online brokers");
    expect(await page.innerText("body")).toContain("Consumer groups");
    expect(await page.innerText("body")).toContain("Kafka version");
    expect(await page.innerText("body")).toContain("Used disk space");
    expect(await page.innerText("body")).toContain("CPU usage");
    expect(await page.innerText("body")).toContain("Memory usage");
    expect(await page.innerText("body")).toContain("Topic metrics");
    expect(await page.innerText("body")).toContain(
      "Topics bytes incoming and outgoing",
    );
  });
});
