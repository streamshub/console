import { expect, test } from "@playwright/test";

test("Partitions page", async ({page}) => {
  await test.step("Navigate to partitions page", async () => {
    await page.goto("./home");
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
    await page.click('table[aria-label="Topics"] tbody tr:first-child td:first-child a');
    await page.waitForSelector('text="No messages data"'||'text="messages=latest retrieve=50"', { timeout: 500000 });
    await page.click('text="Partitions"');
    await page.waitForSelector('text="Partition ID"', { timeout: 500000 });
   
  })
  await test.step("Partitions page should display table", async () => {
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Partition ID");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Replicas");
    expect(await page.innerText("body")).toContain("Size");
    expect(await page.innerText("body")).toContain("Leader");
    expect(await page.innerText("body")).toContain("Preferred leader");
    const dataRows = await page.locator('table[aria-label="Partitions"] tbody tr').elementHandles();
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.locator('table[aria-label="Partitions"] tbody tr td').evaluateAll((tds) =>
    tds.map((td) => td.textContent?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
