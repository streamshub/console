import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToTopics();
});

test("Messages page with messages", async ({ page }) => {
  await test.step("Navigate to topic with data", async () => {
    page
      .locator(`table[data-ouia-component-id="topics-listing"] tbody tr`)
      .filter({ hasNotText: "0 B" })
      .first()
      .locator(`td[data-label="Name"] a`)
      .first()
      .click();
  });

  await test.step("Messages page should display table", async () => {
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

test("Messages page without messages", async ({ page }) => {
  await test.step("Navigate to topic without data", async () => {
    page
      .locator(`table[data-ouia-component-id="topics-listing"] tbody tr`)
      .filter({ hasText: "0 B" })
      .first()
      .locator(`td[data-label="Name"] a`)
      .first()
      .click();
  });

  await test.step("Messages page should no messages", async () => {
    await expect(page.getByText("No messages data")).toBeVisible();
    expect(await page.innerText("body")).toContain(
      "Data will appear shortly after we receive produced messages.",
    );
  });  
});
