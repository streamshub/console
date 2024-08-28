import { expect, test } from "@playwright/test";

test("Consumer groups page", async ({ page }) => {
  await test.step("Navigate to consumers group page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Consumer groups"');
    await page.waitForSelector('text="Consumer group name"', {
      timeout: 500000,
    });
  });
  await test.step("Consumer groups page should display table", async () => {
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Consumer group name");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("Members");
    expect(await page.innerText("body")).toContain("Topics");
    const dataRows = await page.locator('table[aria-label="Consumer groups"] tbody tr').count();
    expect(dataRows).toBeGreaterThan(0);

    const dataCells = await page.locator('table[aria-label="Consumer groups"] tbody tr td').evaluateAll((tds) =>
      tds.map((td) => td.textContent?.trim() ?? "")
    );

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
