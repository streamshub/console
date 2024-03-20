import { expect, test } from "@playwright/test";

test("Brokers page", async ({page}) => {
  await test.step("Navigate to brokers page", async () => {
    await page.goto("./home");
    await page.click('text="Brokers"');
    await page.waitForSelector('text="Rack"', { timeout: 500000 });
  })
  await test.step("Brokers page should display table", async () => {
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Brokers");
    expect(await page.innerText("body")).toContain(
      "Partitions distribution (% of total)",
    );
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Total Replicas");
    expect(await page.innerText("body")).toContain("Rack");
    expect(await page.innerText("body")).toContain("Broker ID");
    const dataRows = await page.locator('table[aria-label="Kafka clusters"] tbody tr').count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page.locator('table[aria-label="Kafka clusters"] tbody tr td').evaluateAll((tds) =>
      tds.map((td) => td.textContent?.trim() ?? "")
    );
    
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
