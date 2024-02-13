import { page } from "../../jest.setup";
describe("Topic Configuration", () => {
  test("Topic Configuration form should appear", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/g0WUoQbGROiuHeXnjxGC6Q/configuration"
    );
    await page.waitForLoadState("networkidle");
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
  });
});
