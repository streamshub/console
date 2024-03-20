import { expect, test } from "@playwright/test";

test.describe("Create Topic", () => {
  test.skip("Create Topic form should appear", async ({ page }) => {
    await page.goto(`./kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/create`);
    await page.waitForLoadState("networkidle", { timeout: 10000 });
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
    await page.click("#step-options");

    expect(await page.innerText("body")).toContain(
      "Configure other topic configuration options",
    );
    const optionsScreenshot = await page.screenshot();
    expect(optionsScreenshot).toMatchSnapshot();
    await page.click("#step-review");
    expect(await page.innerText("body")).toContain("Review your topic");
    const reviewScreenshot = await page.screenshot();
    expect(reviewScreenshot).toMatchSnapshot();
  });
});
