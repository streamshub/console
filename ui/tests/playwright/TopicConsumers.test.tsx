import { expect, test } from "@playwright/test";

test("Topics consumers", async ({page}) => {
  await test.step("Navigate to topics consumers page", async () => {
    await page.goto("./home");
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
    await page.click('table[aria-label="Topics"] tbody tr:first-child td:first-child a');
    await page.waitForSelector('text="No messages data"'||'text="messages=latest retrieve=50"', { timeout: 500000 });
    await page.click('text="Consumer groups"');
    await page.waitForSelector('text="Consumer group name"'||'text="No consumer groups"', { timeout: 500000 });   
  })
  await test.step("Topics consumers page should display table", async () => {
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Consumer group name");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Topics");
    expect(await page.innerText("body")).toContain("Members");
    const dataRows = await page.$$(
      'table[aria-label="Consumer groups"] tbody tr',
    );
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Consumer groups"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? ""),
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
