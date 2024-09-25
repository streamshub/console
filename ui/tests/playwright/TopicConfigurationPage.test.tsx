import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Topics configuration", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to topics configuration page", async () => {
    await authenticatedPage.clickLink("Configuration");
  });
  await test.step("Topics configuration page should display table", async () => {
    await authenticatedPage.waitForTableLoaded();
    const dataRows = await page
      .locator('table[aria-label="Node configuration"] tbody tr')
      .elementHandles();
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[aria-label="Node configuration"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.textContent?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
