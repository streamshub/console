import { page } from "../../jest.setup";
import {URL} from './utils'
describe("Topic Configuration", () => {
  test("Topic Configuration form should appear", async () => {
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/ifT6uNQ9QyeVSDEnd9S9Zg/configuration`
    );
    await page.waitForLoadState("networkidle");
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
  });
});
