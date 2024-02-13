import { page } from "../../jest.setup";
describe("Messages page", () => {
  test("Messages page should display table", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/g0WUoQbGROiuHeXnjxGC6Q/messages"
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Timestamp (UTC)");
    expect(await page.innerText("body")).toContain("Key");
    expect(await page.innerText("body")).toContain("Value");
    expect(await page.innerText("body")).toContain("Latest messages");
    expect(await page.innerText("body")).toContain("All partitions");
    const dataRows = await page.$$(
      'table[aria-label="Messages table"] tbody tr'
    );
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Messages table"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
