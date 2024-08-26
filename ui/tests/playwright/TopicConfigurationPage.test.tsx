import { expect, test } from "@playwright/test";

test("Topics configuration", async ({page}) => {
  await test.step("Navigate to topics configuration page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
    await page.click('table[aria-label="Topics"] tbody tr:first-child td:first-child a');
    await expect(page.getByText("Last updated").or(page.getByText("No messages data"))).toBeVisible();
    await page.click('text="Configuration"');
    await page.waitForSelector('text="Clear all filters"', { timeout: 500000 });
  })
  await test.step("Topics configuration page should display table", async () => {
    const dataRows = await page.locator('table[aria-label="Node configuration"] tbody tr').elementHandles();
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.locator('table[aria-label="Node configuration"] tbody tr td').evaluateAll((tds) =>
    tds.map((td) => td.textContent?.trim() ?? "")
  );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
