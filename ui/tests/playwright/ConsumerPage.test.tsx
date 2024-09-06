import { expect, test } from "@playwright/test";

test("Consumer  page", async ({page}) => {
  await test.step("Navigate to consumer page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Consumer groups"');
    await page.click('table[aria-label="Consumer groups"] tbody.pf-v5-c-table__tbody > tr:nth-of-type(1) td.pf-v5-c-table__td:nth-of-type(1) a');
    await page.waitForSelector('text="Member ID"', { timeout: 500000 });
  })
  await test.step("Consumer  page should display table", async () => {
    expect(await page.innerText("body")).toContain("Member ID");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("Assigned partitions");
    const button = page.locator('button[aria-labelledby="simple-node0 00"][aria-label="Details"]').first();
    await button?.click();
    expect(await page.innerText("body")).toContain("Committed offset");
    expect(await page.innerText("body")).toContain("Topic");
    expect(await page.innerText("body")).toContain("Partition");
    expect(await page.innerText("body")).toContain("Lag");
    expect(await page.innerText("body")).toContain("End offset");
  });
});
