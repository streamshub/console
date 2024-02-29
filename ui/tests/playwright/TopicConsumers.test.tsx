import { page } from "../../jest.setup";
import {URL} from './utils'

describe("Topic Consumers", () => {
  test("Topic Consumers page should display table", async () => {
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/ifT6uNQ9QyeVSDEnd9S9Zg/consumer-groups`
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Consumer group name");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("State");
    expect(await page.innerText("body")).toContain("Topics");
     expect(await page.innerText("body")).toContain("Members");
    const dataRows = await page.$$(
      'table[aria-label="Consumer groups"] tbody tr'
    );
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Consumer groups"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
