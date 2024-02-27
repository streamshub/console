import { page } from "../../jest.setup";
import {URL} from './utils'

describe("Create Topic", () => {
  test.skip("Create Topic form should appear", async () => {
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/create`
    );
    await page.waitForLoadState("networkidle", { timeout: 10000 });
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
