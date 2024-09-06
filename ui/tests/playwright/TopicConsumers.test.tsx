import { expect, test } from "@playwright/test";

test("Topics consumers", async ({page}) => {
  await test.step("Navigate to topics consumers page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
    await page.click('table[aria-label="Topics"] tbody tr:first-child td:first-child a');
    await expect(page.getByText("Last updated").or(page.getByText("No messages data"))).toBeVisible();
    await page.click('text="Consumer groups"');
    await expect(page.getByText("Consumer group name").or(page.getByText("No consumer groups"))).toBeVisible();
  })
  await test.step("Topics consumers page should display table", async () => {
    expect(await page.innerText("body")).toContain("Consumer group name");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Topics");
    expect(await page.innerText("body")).toContain("Members");

    await page.waitForSelector('table[aria-label="Consumer groups"] tbody tr');
    const dataRows = await page.locator('table[aria-label="Consumer groups"] tbody tr').count();
    expect(dataRows).toBeGreaterThan(0);

    const dataCells = await page.locator('table[aria-label="Consumer groups"] tbody tr td').evaluateAll((tds) =>
      tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
