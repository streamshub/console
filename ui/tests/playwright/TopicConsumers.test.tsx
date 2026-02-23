import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Topics consumers", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to topic groups page", async () => {
    await authenticatedPage.clickTab("Groups");
  });
  await test.step("Topics consumers page should display table", async () => {
    await authenticatedPage.waitForTableLoaded();
    if (await page.getByText("No groups").isVisible()) {
      return;
    }
    expect(await page.innerText("body")).toContain("Group ID");
    expect(await page.innerText("body")).toContain("Type");
    expect(await page.innerText("body")).toContain("Protocol");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("Members");
    expect(await page.innerText("body")).toContain("Topics");

    await page.waitForSelector('table[aria-label="Groups"] tbody tr');
    const dataRows = await page
      .locator('table[aria-label="Groups"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);

    const dataCells = await page
      .locator('table[aria-label="Groups"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.Content?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
