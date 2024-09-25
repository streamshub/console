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
    expect(await page.innerText("body")).toContain("Consumer group name");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("Members");
    expect(await page.innerText("body")).toContain("Topics");
    const dataRows = await page
      .locator('table[aria-label="Consumer groups"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);

    const dataCells = await page
      .locator('table[aria-label="Consumer groups"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.textContent?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
