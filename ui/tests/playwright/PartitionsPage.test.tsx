import { expect, test } from "@playwright/test";

test.describe("Partitions page", () => {
  test("Partitions page should display table", async ({ page }) => {
    await page.goto(
      `./kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/ifT6uNQ9QyeVSDEnd9S9Zg/partitions`,
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Partition ID");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Replicas");
    expect(await page.innerText("body")).toContain("Size");
    expect(await page.innerText("body")).toContain("Leader");
    expect(await page.innerText("body")).toContain("Preferred leader");
    const dataRows = await page.$$('table[aria-label="Partitions"] tbody tr');
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Partitions"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? ""),
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
