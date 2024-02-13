import { page } from "./setup";
describe("Brokers property page", () => {
  test("Brokers property page should display table", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/nodes/0/configuration"
    );
    await page.waitForLoadState("networkidle");
    const screenshot = await page.screenshot();
    expect(screenshot).toMatchSnapshot();
  });
});
