import { expect, test } from "@playwright/test";

test.describe("Home page", () => {
  test("Column headings are present", async ({ page }) => {
    await page.goto(`./home`);
    const columnHeadings = await page.$$eval(
      'table[aria-label="Kafka clusters"] thead th',
      (ths) => ths.map((th) => th.textContent?.trim()),
    );
    expect(columnHeadings).toContain("Name");
    expect(columnHeadings).toContain("Brokers");
    expect(columnHeadings).toContain("Consumer groups");
    expect(columnHeadings).toContain("Kafka version");
    expect(columnHeadings).toContain("Project");
  });

  test("Connection details should show URL", async ({ page }) => {
    // Click on the kebab toggle button
    await page.click('button[class="pf-v5-c-menu-toggle pf-m-plain"]');
    // Wait for the dropdown menu to appear
    await page.waitForSelector('ul[class="pf-v5-c-menu__list"]');
    // Click on the "Connection details" button within the dropdown menu
    await page.click(
      'button[class="pf-v5-c-menu__item"] span[class="pf-v5-c-menu__item-text"]:has-text("Connection details")',
    );
    await page.waitForSelector('.pf-v5-c-form-control input[type="text"]');
    expect(await page.innerText("body")).toContain(
      "Cluster connection details",
    );
    expect(await page.innerText("body")).toContain(
      "External listeners provide client access to a Kafka cluster from outside the OpenShift cluster.",
    );
    const inputValues = await page.$$eval(
      '.pf-v5-c-form-control input[type="text"]',
      (inputs: HTMLInputElement[]) => inputs.map((input) => input.value),
    );
    expect(inputValues.every((value) => value.length > 1)).toBe(true); //await page.waitForSelector('span[class="pf-v5-c-form-control pf-m-readonly"] input[id="text-input-13"]');
  });

  test("Data rows are present", async ({ page }) => {
    const dataRows = await page.$$(
      'table[aria-label="Kafka clusters"] tbody tr',
    );
    expect(dataRows.length).toBeGreaterThan(0);
  });

  test("Data cells in each row are present", async ({ page }) => {
    const dataCells = await page.$$eval(
      'table[aria-label="Kafka clusters"] tbody tr td',
      (tds) => tds.map((td) => td.textContent?.trim() ?? ""),
    );
    expect(dataCells.length).toBeGreaterThan(0);
  });

  test("Text on Home Page", async ({ page }) => {
    // Assertions for the presence of specific strings in the inner text of page elements
    expect(await page.innerText("body")).toContain(
      "Welcome to the AMQ streams console",
    );
    expect(await page.innerText("body")).toContain(
      "Platform: OpenShift Cluster",
    );
    expect(await page.innerText("body")).toContain("Recently viewed topics");
    expect(await page.innerText("body")).toContain(
      'When you start looking at specific topics through the AMQ Streams console, they"ll start showing here.',
    );
    expect(await page.innerText("body")).toContain(
      "AMQ Streams on OpenShift Overview",
    );
    expect(await page.innerText("body")).toContain(
      "Recommended learning resources",
    );
    expect(await page.innerText("body")).toContain(
      "Getting Started with AMQ Streams on Openshift",
    );
    expect(await page.innerText("body")).toContain(
      "Connect to a Kafka cluster from an application",
    );
    expect(await page.innerText("body")).toContain(
      "Using the Topic Operator to manage Kafka topics",
    );
  });
});
