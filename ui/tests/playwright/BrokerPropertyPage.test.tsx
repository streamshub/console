import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Brokers property page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to brokers property page", async () => {
    await page.click('text="Brokers"');
    await authenticatedPage.waitForTableLoaded();
    await authenticatedPage.clickFirstLinkInTheTable("Kafka clusters");
  });
  await test.step("Brokers page should display properties", async () => {
    await authenticatedPage.waitForTableLoaded();
    const dataRows = await page
      .locator('table[aria-label="Node configuration"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[aria-label="Node configuration"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.textContent?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
