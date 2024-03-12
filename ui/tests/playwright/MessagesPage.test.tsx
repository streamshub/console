import { expect, test } from "@playwright/test";

test("Messages page", async ({page}) => {
  await test.step("Navigate to topics messages page", async () => {
    await page.goto("./home");
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
    await page.click('table[aria-label="Topics"] tbody tr:first-child td:first-child a');
    await page.waitForSelector('text="No messages data"'||'text="messages=latest retrieve=50"', { timeout: 500000 });
  })
  await test.step("Messages page should display table", async () => {
    await page.waitForLoadState("networkidle");
    try{
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
    }
    catch (e) {
      expect(await page.innerText("body")).toContain("No messages data");
      expect(await page.innerText("body")).toContain("Data will appear shortly after we receive produced messages.");
    }
  });
});
