import { expect, test } from './authenticated-test';

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToTopics();
});

test('Messages page with messages', async ({ page }) => {
  await test.step('Navigate to topic with data', async () => {
    await page
      .locator(`table[data-ouia-component-id="topics-table"] tbody tr`)
      .filter({ hasNotText: '0 B' })
      .first()
      .locator(`td[data-label="Name"] a`)
      .first()
      .click();
  });

  await test.step('Messages page should display table', async () => {
    await expect(page.getByRole('columnheader', { name: 'Key' })).toBeVisible();
    await expect(
      page.getByRole('columnheader', { name: 'Value' })
    ).toBeVisible();
  });

  await test.step('Filter is visible with its defaults', async () => {
    await expect(
      page.getByRole('button', { name: 'All partitions' })
    ).toBeVisible();
    await expect(page.getByRole('button', { name: 'Latest' })).toBeVisible();
    await expect(
      page.getByRole('button', { name: '50', exact: true })
    ).toBeVisible();
  });

  await test.step('Table shows messages', async () => {
    const table = page.locator('table[aria-label="Messages table"]');

    // While loading, the table body holds skeleton rows, which are also <tr>
    // elements. Wait for a real message instead of counting rows.
    await expect(table.getByText(/^Partition \d+$/).first()).toBeVisible();

    const dataCells = await table
      .locator('tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ''));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});

test('Messages table keeps its header and filter when the filter changes', async ({
  page,
}) => {
  await test.step('Navigate to topic with data', async () => {
    await page
      .locator(`table[data-ouia-component-id="topics-table"] tbody tr`)
      .filter({ hasNotText: '0 B' })
      .first()
      .locator(`td[data-label="Name"] a`)
      .first()
      .click();
  });

  await test.step('Change the limit', async () => {
    await page.getByRole('button', { name: '50', exact: true }).click();
    await page.getByRole('option', { name: '100', exact: true }).click();
  });

  await test.step('Header and filter stay, the choice is kept', async () => {
    await expect(page.getByRole('columnheader', { name: 'Key' })).toBeVisible();
    await expect(
      page.getByRole('button', { name: '100', exact: true })
    ).toBeVisible();
    await expect(page).toHaveURL(/retrieve=100/);
  });
});

test('Messages page without messages', async ({ page }) => {
  await test.step('Navigate to topic without data', async () => {
    await page
      .locator(`table[data-ouia-component-id="topics-table"] tbody tr`)
      .filter({ hasText: '0 B' })
      .first()
      .locator(`td[data-label="Name"] a`)
      .first()
      .click();
  });

  await test.step('Messages page should show no messages in the table', async () => {
    await expect(page.getByText('No messages data')).toBeVisible();
    await expect(
      page.getByText(
        'Data will appear shortly after we receive produced messages.'
      )
    ).toBeVisible();

    // The empty message is inside the table body, so the header stays
    await expect(page.getByRole('columnheader', { name: 'Key' })).toBeVisible();
  });
});
