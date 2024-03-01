import { expect, test } from "@playwright/test";

test.describe("Brokers property page", () => {
  test("Brokers property page should display table", async ({ page }) => {
    await page.goto(`./kafka/j7W3TRG7SsWCBXHjz2hfrg/nodes/0/configuration`);
    await page.waitForLoadState("networkidle");
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
  });
});
