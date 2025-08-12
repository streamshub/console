import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Consumer groups page", async ({ page }) => {
  await test.step("Consumer groups page should display table", async () => {
    await page.waitForFunction(() => {
      return (
        document.querySelectorAll(
          'table[aria-label="Consumer groups"] tbody tr',
        ).length > 0
      );
    });

    await expect(page.getByRole('columnheader', { name: "Consumer group name" })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: "State" })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: "Overall lag" })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: "Members" })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: "Topics" })).toBeVisible();

    const dataCells = await page
      .locator('table[aria-label="Consumer groups"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
