import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Topics configuration", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to topics configuration page", async () => {
    await authenticatedPage.clickTab("Configuration");
    await expect(page.getByRole('columnheader', { name: 'Property' })).toBeVisible();
  });
  await test.step("Topics configuration page should display table", async () => {
    await page.waitForFunction(() => {
      return (
        document.querySelectorAll(
          'table[aria-label="Topic configuration"] tbody tr',
        ).length > 0
      );
    });

    const dataCells = await page
      .locator('table[aria-label="Topic configuration"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
