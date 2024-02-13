import { page } from "./setup";
describe("Create Topic", () => {
  test("Create Topic form should appear", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/create"
    );
    await page.waitForLoadState("networkidle");
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
    await page.click("#step-options");

    expect(await page.innerText("body")).toContain(
      "Configure other topic configuration options"
    );
    const optionsScreenshot = await page.screenshot();
    expect(optionsScreenshot).toMatchSnapshot();
    await page.click("#step-review");
    expect(await page.innerText("body")).toContain("Review your topic");
    const reviewScreenshot = await page.screenshot();
    expect(reviewScreenshot).toMatchSnapshot();
  });
});
