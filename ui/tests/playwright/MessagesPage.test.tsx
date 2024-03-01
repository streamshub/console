import { expect, test } from "@playwright/test";

test.describe("Messages page", () => {
  test("Messages page should display table", async ({ page }) => {
    await page.goto(
      `./kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/ifT6uNQ9QyeVSDEnd9S9Zg/messages`,
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Key");
    expect(await page.innerText("body")).toContain("All partitions");
    const dataRows = await page.$$(
      'table[aria-label="Messages table"] tbody tr',
    );
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Messages table"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? ""),
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
