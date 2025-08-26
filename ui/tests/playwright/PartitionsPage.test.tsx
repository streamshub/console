import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test("Partitions page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to partitions page", async () => {
    await authenticatedPage.clickTab("Partitions");
    await expect(page.getByRole('columnheader', { name: 'Partition ID' })).toBeVisible();
  });
  await test.step("Partitions page should display table", async () => {
    expect(await page.innerText("body")).toContain("Partition ID");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Replicas");
    expect(await page.innerText("body")).toContain("Size");
    expect(await page.innerText("body")).toContain("Leader");
    expect(await page.innerText("body")).toContain("Preferred leader");

    await page.waitForFunction(() => {
      return (
        document.querySelectorAll(
          'table[aria-label="Partitions"] tbody tr',
        ).length > 0
      );
    });

    const dataCells = await page
      .locator('table[aria-label="Partitions"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
