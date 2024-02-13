import { page } from "./setup";
describe("Topics page", () => {
  test("Topics page should display table", async () => {
    await page.goto(
      "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/kafka/j7W3TRG7SsWCBXHjz2hfrg/topics"
    );
    await page.waitForLoadState("networkidle");
    const label = await page.$("label.pf-v5-c-switch");
    expect(label).not.toBeNull();
    const labelText = await label?.innerText();
    expect(labelText?.trim()).toBe("Hide internal topics");
    const input = await label?.$("input.pf-v5-c-switch__input");
    expect(input).not.toBeNull();
    const isChecked = await input?.isChecked();
    expect(isChecked).toBe(true);
    const button = await page.$('button:has-text("Create Topic")');
    expect(button).not.toBeNull();
    const filterInput = await page.$("div.pf-v5-c-input-group input");
    expect(filterInput).not.toBeNull();

    // Get the placeholder attribute value
    const placeholderValue = await filterInput?.getAttribute("placeholder");
    expect(placeholderValue).toBe("Filter by name");
    expect(await page.innerText("body")).toContain("Name");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Partitions");
    expect(await page.innerText("body")).toContain("Consumer groups");
    expect(await page.innerText("body")).toContain("Storage");
    const dataRows = await page.$$('table[aria-label="Topics"] tbody tr');
    expect(dataRows.length).toBeGreaterThan(0);
    const dataCells = await page.$$eval(
      'table[aria-label="Topics"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? "")
    );
    expect(dataCells.length).toBeGreaterThan(0);
    //Test the presence of kebab dropdown menu
    await page.click('button[class="pf-v5-c-menu-toggle pf-m-plain"]');
    await page.waitForSelector('ul[class="pf-v5-c-menu__list"]');
    expect(await page.innerText("body")).toContain("Edit configuration");
    expect(await page.innerText("body")).toContain("Delete topic");
  });
});
