import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Nodes page", async ({ page }) => {
  await test.step("Navigate to nodes page", async () => {
    await page.click('text="Kafka nodes"');
    await page.waitForSelector('text="Rack"', { timeout: 500000 });
  });
  await test.step("Nodes page should display table", async () => {
    expect(await page.innerText("body")).toContain("Nodes");
    expect(await page.innerText("body")).toContain(
      "Partitions distribution (% of total)",
    );
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Total Replicas");
    expect(await page.innerText("body")).toContain("Rack");
    expect(await page.innerText("body")).toContain("Node ID");
    const dataRows = await page
      .locator('table[aria-label="Kafka nodes"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[aria-label="Kafka nodes"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.textContent?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
