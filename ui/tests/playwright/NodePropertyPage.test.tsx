import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Node property page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to node property page", async () => {
    await page.click('text="Kafka nodes"');
    await authenticatedPage.waitForTableLoaded();
    await authenticatedPage.clickFirstLinkInTheTable("Kafka nodes");
  });
  await test.step("Node page should display properties", async () => {
    await authenticatedPage.waitForTableLoaded();
    const dataRows = await page
      .locator('table[aria-label="Node configuration"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[aria-label="Node configuration"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.Content?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
