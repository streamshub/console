import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Nodes page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to nodes page", async () => {
    await authenticatedPage.clickLink('Kafka Nodes', "sidebar");
    await expect(page.getByRole('columnheader', { name: 'Rack' })).toBeVisible();
  });
  await test.step("Nodes page should display table", async () => {
    await expect(page.locator('h1').getByText('Nodes')).toBeVisible();

    const headerRows = await page
      .locator('table[data-ouia-component-id="nodes-table"] thead tr')
      .all();
    const headerRow = headerRows[0];
    let col = 0;
    expect(await headerRow.locator("th").nth(col++).innerText()).toBe("Node ID");
    expect(await headerRow.locator("th").nth(col++).innerText()).toBe("Roles");
    expect(await headerRow.locator("th").nth(col++).innerText()).toBe("Status");
    expect(await headerRow.locator("th").nth(col++).innerText()).toBe("Kafka version");
    expect(await headerRow.locator("th").nth(col++).innerText()).toContain(
      "Total Replicas ",
    );
    expect(await headerRow.locator("th").nth(col++).innerText()).toContain(
      "Leader partitions ",
    );
    expect(await headerRow.locator("th").nth(col++).innerText()).toContain("Rack ");
    expect(await headerRow.locator("th").nth(col++).innerText()).toBe("Node Pool");

    const dataRows = await page
      .locator('table[data-ouia-component-id="nodes-table"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[data-ouia-component-id="nodes-table"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
