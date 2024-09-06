import { expect, test } from "@playwright/test";

test("Messages page", async ({page}) => {
  await test.step("Navigate to topics messages page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
    await page.click('table[aria-label="Topics"] tbody tr:first-child td:first-child a');
    await expect(page.getByText("Last updated").or(page.getByText("No messages data"))).toBeVisible();
  })
  await test.step("Messages page should display table", async () => {
    if (await page.getByText("No messages data").isVisible()) {
      expect(await page.innerText("body")).toContain("Data will appear shortly after we receive produced messages.");
      return;
    }

    expect(await page.innerText("body")).toContain("Key");
    await page.click('button[aria-label="Open advanced search"]');
    expect(await page.innerText("body")).toContain("All partitions");
    const dataRows = await page.locator('table[aria-label="Messages table"] tbody tr').elementHandles();
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.locator('table[aria-label="Messages table"] tbody tr td').evaluateAll((tds) =>
      tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
