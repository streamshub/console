import { page } from "../../jest.setup";
import {URL} from './utils'

describe("Messages page", () => {
  test("Messages page should display table", async () => {
    jest.setTimeout(30000)
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/RRf0He61TRWcMgG7qksTiw/messages`
    );
    await page.waitForLoadState("networkidle",{timeout: 60000});
    expect(await page.innerText("body")).toContain("Key");
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
