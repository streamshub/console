import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Consumer groups page", async ({ page }) => {
  await test.step("Consumer groups page should display table or empty state", async () => {
    const tableRows = page.locator(
      'table[aria-label="Consumer groups"] tbody tr'
    );
    const emptyState = page.getByText("No consumer groups");

    await Promise.race([
      tableRows.first().waitFor({ state: "visible" }).catch(() => null),
      emptyState.waitFor({ state: "visible" }).catch(() => null),
    ]);

    if (await emptyState.isVisible()) {
      await expect(emptyState).toBeVisible();
      return; 
    }

    await expect(page.getByRole("columnheader", { name: "Consumer group name" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "State" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Overall lag" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Members" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Topics" })).toBeVisible();

    const dataCells = await page
      .locator('table[aria-label="Consumer groups"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
