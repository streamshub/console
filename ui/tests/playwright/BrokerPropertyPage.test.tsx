import { expect, test } from "@playwright/test";

test("Brokers property page", async ({page}) => {
  await test.step("Navigate to brokers property page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Brokers"');
    await page.waitForSelector('text="Broker ID"',{ timeout: 500000 });
    await page.click('table[aria-label="Kafka clusters"] tbody tr:nth-child(1) td:nth-child(2) a');
    await page.waitForSelector('text="Clear all filters"',{ timeout: 500000 });
  })
  await test.step("Brokers page should display properties", async () => {
    const dataRows = await page.locator('table[aria-label="Node configuration"] tbody tr').count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page.locator('table[aria-label="Node configuration"] tbody tr td').evaluateAll((tds) =>
      tds.map((td) => td.textContent?.trim() ?? "")
    );

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
