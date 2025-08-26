import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Messages page", async ({ page }) => {
  await test.step("Messages page should display table, or an empty state", async () => {
    if (await page.getByText("No messages data").isVisible()) {
      expect(await page.innerText("body")).toContain(
        "Data will appear shortly after we receive produced messages.",
      );
      return;
    }

    await expect(page.getByRole('columnheader', { name: 'Key' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Value' })).toBeVisible();

    await page.getByLabel('Open advanced search').click();
    await expect(page.getByRole('button', { name: 'All partitions' })).toBeVisible();

    await page.waitForFunction(() => {
          return (
            document.querySelectorAll(
              'table[aria-label="Messages table"] tbody tr',
            ).length > 0
          );
        });

    const dataCells = await page
      .locator('table[aria-label="Messages table"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
