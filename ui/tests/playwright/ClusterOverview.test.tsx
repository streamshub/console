import { page } from "../../jest.setup";
describe("Cluster Overview page", () => {
  test("Cluster overview page should display correctly", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/overview"
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Cluster overview");
    expect(await page.innerText("body")).toContain(
      "Key performance indicators and important information regarding the Kafka cluster."
    );
    const noMessagesText = await page.$eval('span[id="no-messages"]', (span) =>
      span?.textContent?.trim()
    );
    expect(noMessagesText).toBe("No messages");
    //const page = await page.innerText('div[class="pf-v5-l-flex pf-m-justify-content-center pf-m-wrap pf-m-nowrap-on-sm pf-m-gap"]');
    expect(await page.innerText("body")).toContain("kafka1");
    expect(await page.innerText("body")).toContain("Ready");
    expect(await page.innerText("body")).toContain("3/3");
    expect(await page.innerText("body")).toContain("Online brokers");
    expect(await page.innerText("body")).toContain("0");
    expect(await page.innerText("body")).toContain("Consumer groups");
    expect(await page.innerText("body")).toContain("3.5.0");
    expect(await page.innerText("body")).toContain("Kafka version");
    expect(await page.innerText("body")).toContain("6 GiB");
    expect(await page.innerText("body")).toContain("7 GiB");
    expect(await page.innerText("body")).toContain("9 GiB");
    expect(await page.innerText("body")).toContain("Used disk space");
    expect(await page.innerText("body")).toContain("CPU usage");
    expect(await page.innerText("body")).toContain("Memory usage");
    expect(await page.innerText("body")).toContain("300m");
    expect(await page.innerText("body")).toContain("977 KiB");
    expect(await page.innerText("body")).toContain("Topic metrics");
    expect(await page.innerText("body")).toContain(
      "Topics bytes incoming and outgoing"
    );
    const pageInnerText = await page.innerText("html");
    const chartAxis = "4 GiB";
    const chartAxisRegex = new RegExp(chartAxis, "g");
    const chartAxisOccurances = (pageInnerText.match(chartAxisRegex) || [])
      .length;
    // Count the occurrences of the text
    const node0 = "Node 0";
    const regex = new RegExp(node0, "g");
    const node0Occurrences = (pageInnerText.match(regex) || []).length;

    const node1 = "Node 1";
    const node1Regex = new RegExp(node1, "g");
    const node1Occurrences = (pageInnerText.match(node1Regex) || []).length;

    const node2 = "Node 2";
    const node2regex = new RegExp(node2, "g");
    const node2Occurrences = (pageInnerText.match(node2regex) || []).length;

    expect(node0Occurrences).toBe(3);
    expect(node1Occurrences).toBe(3);
    expect(node2Occurrences).toBe(3);
    expect(chartAxisOccurances).toBe(2);
  });
});
