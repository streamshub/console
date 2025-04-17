import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Messages page", async ({ page, authenticatedPage }) => {
  await test.step("Messages page should display table, or an empty state", async () => {
    if (await page.getByText("No messages data").isVisible()) {
      expect(await page.innerText("body")).toContain(
        "Data will appear shortly after we receive produced messages.",
      );
      return;
    }

    expect(await page.innerText("body")).toContain("Key");
    await page.click('button[aria-label="Open advanced search"]');
    expect(await page.innerText("body")).toContain("All partitions");
    const dataRows = await page
      .locator('table[aria-label="Messages table"] tbody tr')
      .elementHandles();
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[aria-label="Messages table"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.Content?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
