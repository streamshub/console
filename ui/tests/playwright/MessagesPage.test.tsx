import { expect, test } from './authenticated-test';

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToTopics();
});

test('Messages page with messages', async ({ page }) => {
  await test.step('Navigate to topic with data', async () => {
    await page
      .locator('table[data-ouia-component-id="topics-table"] tbody tr')
      .filter({ hasNotText: '0 B' })
      .first()
      .locator('td[data-label="Name"] a')
      .first()
      .click();
  });

  await test.step('Messages page should display table', async () => {
    const messagesTable = page.getByRole('table', {
      name: 'Messages',
    });

    await expect(messagesTable).toBeVisible();

    await expect(
      messagesTable.getByRole('columnheader', { name: 'Key' })
    ).toBeVisible();

    await expect(
      messagesTable.getByRole('columnheader', { name: 'Value' })
    ).toBeVisible();

    const messageRows = messagesTable.locator('tbody tr');

    await expect(messageRows.first()).toBeVisible();

    // The table should contain at least one message row.
    await expect(messageRows.first().locator('td')).not.toHaveCount(0);
  });
});

test('Messages page without messages', async ({ page }) => {
  await test.step('Navigate to topic without data', async () => {
    await page
      .locator('table[data-ouia-component-id="topics-table"] tbody tr')
      .filter({ hasText: '0 B' })
      .first()
      .locator('td[data-label="Name"] a')
      .first()
      .click();
  });

  await test.step('Messages page should show empty state', async () => {
    await expect(page.getByText('No messages data')).toBeVisible();

    await expect(
      page.getByText(
        'Data will appear shortly after we receive produced messages.'
      )
    ).toBeVisible();
  });
});
