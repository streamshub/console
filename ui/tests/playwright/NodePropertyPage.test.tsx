import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Node property page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to node property page", async () => {
    await page.click('text="Kafka nodes"');
    await expect(page.getByRole('columnheader', { name: 'Node ID' })).toBeVisible();
    await authenticatedPage.clickFirstLinkInTheTable("Kafka nodes");
    await expect(page.getByRole('columnheader', { name: 'Property' })).toBeVisible();
  });
  await test.step("Node page should display properties", async () => {
    await page.waitForFunction(() => {
      return (
        document.querySelectorAll(
          'table[aria-label="Node configuration"] tbody tr',
        ).length > 0
      );
    });

    const dataCells = await page
      .locator('table[aria-label="Node configuration"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
