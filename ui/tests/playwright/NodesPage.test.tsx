import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Nodes page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to nodes page", async () => {
    await authenticatedPage.clickLink('Kafka nodes', "sidebar");
    await expect(page.getByRole('columnheader', { name: 'Rack' })).toBeVisible();
  });
  await test.step("Nodes page should display table", async () => {
    await expect(page.locator('h1').getByText('Nodes')).toBeVisible();
    await expect(page.getByText('Partitions distribution (% of total)')).toBeVisible();

    const headerRows = await page
      .locator('table[aria-label="Kafka nodes"] thead tr')
      .all();
    const headerRow = headerRows[0];
    expect(await headerRow.locator("th").nth(1).innerText()).toBe("Node ID");
    expect(await headerRow.locator("th").nth(2).innerText()).toBe("Roles");
    expect(await headerRow.locator("th").nth(3).innerText()).toBe("Status");
    expect(await headerRow.locator("th").nth(4).innerText()).toContain(
      "Total Replicas ",
    );
    expect(await headerRow.locator("th").nth(5).innerText()).toContain("Rack ");
    expect(await headerRow.locator("th").nth(6).innerText()).toBe("Node Pool");

    const dataRows = await page
      .locator('table[aria-label="Kafka nodes"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[aria-label="Kafka nodes"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
