import { expect, test } from './authenticated-test';

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test('Nodes page', async ({ page, authenticatedPage }) => {
  await test.step('Navigate to nodes page', async () => {
    await authenticatedPage.clickLink('Kafka Nodes', 'sidebar');
    await expect(
      page.getByRole('columnheader', { name: 'Rack' })
    ).toBeVisible();
  });
  await test.step('Nodes page should display table', async () => {
    await expect(page.locator('h1').getByText('Nodes')).toBeVisible();

    const headerRows = await page
      .locator('table[data-ouia-component-id="nodes-table"] thead tr')
      .all();
    const headerRow = headerRows[0];
    const headers = headerRow.locator('th');
    expect(await headers.nth(0).innerText()).toBe('Node ID');
    expect(await headers.nth(1).innerText()).toBe('Roles');
    expect(await headers.nth(2).innerText()).toBe('Status');
    expect(await headers.nth(3).innerText()).toBe('Kafka version');
    expect(await headers.nth(4).innerText()).toContain('Total Replicas ');
    expect(await headers.nth(5).innerText()).toContain('Leader partitions ');
    expect(await headers.nth(6).innerText()).toContain('Rack ');
    expect(await headers.nth(7).innerText()).toBe('Node Pool');

    const dataRows = await page
      .locator('table[data-ouia-component-id="nodes-table"] tbody tr')
      .count();
    expect(dataRows).toBeGreaterThan(0);
    const dataCells = await page
      .locator('table[data-ouia-component-id="nodes-table"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ''));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
