import { page } from "../../jest.setup";
import {URL} from './utils'

describe("Consumer page", () => {
  test("Consumer should display table", async () => {
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/consumer-groups/__strimzi-topic-operator-kstreams`
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Member ID");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("Assigned partitions");
    const button = await page.$(
      'button[aria-labelledby="simple-node0 00"][aria-label="Details"]'
    );
    await button?.click();
    expect(await page.innerText("body")).toContain("Committed offset");
    expect(await page.innerText("body")).toContain("Topic");
    expect(await page.innerText("body")).toContain("Partition");
    expect(await page.innerText("body")).toContain("Lag");
    expect(await page.innerText("body")).toContain("End offset");
  });
});
