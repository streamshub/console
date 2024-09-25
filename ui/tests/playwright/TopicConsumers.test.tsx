import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Topics consumers", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to topics consumers page", async () => {
    await authenticatedPage.clickLink("Consumer groups");
  });
  await test.step("Topics consumers page should display table", async () => {
    await authenticatedPage.waitForTableLoaded();
    if (await page.getByText("No consumer groups").isVisible()) {
      return;
    }
    expect(await page.innerText("body")).toContain("Consumer group name");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Topics");
    expect(await page.innerText("body")).toContain("Members");

    await page.waitForSelector('table[aria-label="Consumer groups"] tbody tr');
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
