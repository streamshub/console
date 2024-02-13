import { page } from "../../jest.setup";
describe("Brokers page", () => {
  test("Brokers page should display table", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/nodes"
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Brokers");
    expect(await page.innerText("body")).toContain(
      "Partitions distribution (% of total)"
    );
    expect(await page.innerText("body")).toContain("Broker 0 total partitions");
    expect(await page.innerText("body")).toContain("Broker 1 total partitions");
    expect(await page.innerText("body")).toContain("Broker 2 total partitions");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Total Replicas");
    expect(await page.innerText("body")).toContain("Rack");
    expect(await page.innerText("body")).toContain("Broker ID");
    const dataRows = await page.$$(
      'table[aria-label="Kafka clusters"] tbody tr'
    );
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Kafka clusters"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
