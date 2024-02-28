import { page } from "../../jest.setup";
import {URL} from './utils'

describe("Partitions page", () => {
  jest.setTimeout(30000)
  test("Partitions page should display table", async () => {
    await page.goto(
      `${URL}/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics/RRf0He61TRWcMgG7qksTiw/partitions`
    );
    await page.waitForLoadState("networkidle");
    expect(await page.innerText("body")).toContain("Partition ID");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Replicas");
    expect(await page.innerText("body")).toContain("Size");
    expect(await page.innerText("body")).toContain("Leader");
    expect(await page.innerText("body")).toContain("Preferred leader");
    const dataRows = await page.$$('table[aria-label="Partitions"] tbody tr');
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Partitions"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
