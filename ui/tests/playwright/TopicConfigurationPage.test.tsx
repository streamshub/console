import { page } from "../../jest.setup";
import {URL} from './utils'
describe("Topic Configuration", () => {
  test("Topic Configuration form should appear", async () => {
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/RRf0He61TRWcMgG7qksTiw/configuration`
    );
    await page.waitForLoadState("networkidle",{timeout: 60000});
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
  });
});
