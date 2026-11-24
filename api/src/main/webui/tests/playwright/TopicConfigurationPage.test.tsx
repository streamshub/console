import type { Page } from '@playwright/test';
import { expect, test } from './authenticated-test';

const cleanupProperty = 'cleanup.policy';
const retentionProperty = 'retention.ms';
const cleanupDocumentation = '  Cleanup policy documentation.\n  ';
const retentionDocumentation = 'Retention policy documentation.';

async function mockTopicConfiguration(page: Page) {
  await page.route(/\/api\/kafkas\/[^/]+\/topics\/[^/]+\?/, async (route) => {
    const url = new URL(route.request().url());

    if (!url.searchParams.has('fields[configs]')) {
      await route.continue();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        data: {
          id: 'mock-topic',
          type: 'topics',
          meta: { privileges: ['UPDATE'] },
          attributes: {
            name: 'mock-topic',
            configs: {
              [cleanupProperty]: {
                value: 'delete',
                source: 'DEFAULT_CONFIG',
                type: 'STRING',
                sensitive: false,
                readOnly: false,
                documentation: cleanupDocumentation,
              },
              [retentionProperty]: {
                value: '604800000',
                source: 'DEFAULT_CONFIG',
                type: 'LONG',
                sensitive: false,
                readOnly: false,
                documentation: retentionDocumentation,
              },
            },
          },
        },
      }),
    });
  });
}

function configurationTable(page: Page) {
  return page.locator(
    'table[data-ouia-component-id="topic-configuration-table"]'
  );
}

function configurationRow(page: Page, property: string) {
  return configurationTable(page)
    .locator('tbody')
    .filter({ hasText: property });
}

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test('Topics configuration', async ({ page, authenticatedPage }) => {
  await test.step('Navigate to topics configuration page', async () => {
    await authenticatedPage.clickTab('Configuration');
    await authenticatedPage.waitForTableLoaded();
    await expect(
      page.getByRole('columnheader', { name: 'Property' })
    ).toBeVisible();
  });
  await test.step('Sortable headers expose correct sort metadata', async () => {
    await expect(
      page.getByRole('columnheader', { name: 'Property' })
    ).toHaveAttribute('aria-sort', 'ascending');
    await expect(
      page.getByRole('columnheader', { name: 'Source' })
    ).toBeVisible();
    await expect(
      page.getByRole('columnheader', { name: 'Value' })
    ).not.toHaveAttribute('aria-sort');
  });
  await test.step('Topics configuration page should display table', async () => {
    await page.waitForFunction(() => {
      return (
        document.querySelectorAll(
          'table[data-ouia-component-id="topic-configuration-table"] tbody tr'
        ).length > 0
      );
    });

    const dataCells = await page
      .locator(
        'table[data-ouia-component-id="topic-configuration-table"] tbody tr td'
      )
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ''));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});

test('Topic configuration expands documentation beneath the matching property row', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfiguration(page);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  const cleanupRows = configurationRow(page, cleanupProperty);
  const retentionRows = configurationRow(page, retentionProperty);
  const cleanupToggle = cleanupRows.getByRole('button', { name: 'Details' });
  const retentionToggle = retentionRows.getByRole('button', {
    name: 'Details',
  });
  const cleanupDocs = cleanupRows.getByTestId(
    'config-documentation-content'
  );
  const retentionDocs = retentionRows.getByTestId(
    'config-documentation-content'
  );

  await test.step('Show an expander for the configuration property', async () => {
    await expect(cleanupToggle).toBeVisible();
    await expect(cleanupDocs).toBeHidden();
  });

  await test.step('Expand the correct row with trimmed documentation', async () => {
    await cleanupToggle.click();
    await expect(cleanupDocs).toBeVisible();
    await expect(cleanupDocs.locator('xpath=ancestor::td')).toHaveAttribute('colspan', '5');
    await expect(cleanupDocs).toContainText(cleanupDocumentation.trim());

    const rawDocumentation = await cleanupDocs
      .locator('div')
      .last()
      .evaluate((element) => element.textContent);
    expect(rawDocumentation).toBe(cleanupDocumentation.trim());

    await expect(retentionDocs).toBeHidden();
  });

  await test.step('Expand another row without mixing documentation', async () => {
    await retentionToggle.click();
    await expect(retentionDocs).toBeVisible();
    await expect(retentionDocs).toContainText(retentionDocumentation);
    await expect(cleanupDocs).not.toContainText(retentionDocumentation);
  });

  await test.step('Collapse the documentation again', async () => {
    await cleanupToggle.click();
    await expect(cleanupDocs).toBeHidden();

    await retentionToggle.click();
    await expect(retentionDocs).toBeHidden();
  });
});

test('Topic configuration preserves property, value, and actions while expanded', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfiguration(page);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  const cleanupRows = configurationRow(page, cleanupProperty);
  const cleanupRow = cleanupRows.locator('tr').first();
  const cleanupToggle = cleanupRows.getByRole('button', { name: 'Details' });
  const cleanupDocs = cleanupRows.getByTestId(
    'config-documentation-content'
  );
  const propertyName = cleanupRow.getByText(cleanupProperty, { exact: true });
  const propertyValue = cleanupRow.getByText('delete', { exact: true });
  const sourceCell = cleanupRow.getByRole('gridcell', { name: 'DEFAULT_CONFIG', exact: true });
  const editAction = cleanupRow.getByRole('button', { name: 'Edit' });

  await expect(propertyName).toBeVisible();
  await expect(propertyValue).toBeVisible();
  await expect(sourceCell).toBeVisible();
  await expect(editAction).toBeVisible();

  const beforeExpansion = await cleanupRow.innerText();

  await cleanupToggle.click();
  await expect(cleanupDocs).toBeVisible();

  await expect(propertyName).toBeVisible();
  await expect(propertyValue).toBeVisible();
  await expect(sourceCell).toBeVisible();
  await expect(editAction).toBeVisible();
  await expect(editAction).toBeEnabled();
  expect(await cleanupRow.innerText()).toBe(beforeExpansion);
});

async function mockTopicPatch(page: Page, status: number) {
  await page.route(/\/api\/kafkas\/[^/]+\/topics\/[^/]+$/, async (route) => {
    if (route.request().method() !== 'PATCH') {
      await route.continue();
      return;
    }

    await route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(
        status === 200 ? {} : { errors: [{ status: String(status), detail: 'Unable to save' }] }
      ),
    });
  });
}

test('Topic inline editing works while documentation is expanded', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfiguration(page);
  await mockTopicPatch(page, 200);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  const cleanupRows = configurationRow(page, cleanupProperty);
  const cleanupRow = cleanupRows.locator('tr').first();
  const docs = cleanupRows.getByTestId('config-documentation-content');
  const edit = cleanupRow.getByRole('button', { name: 'Edit' });

  await edit.click();
  await expect(cleanupRow.getByRole('textbox')).toBeVisible();
  await expect(cleanupRow.getByRole('button', { name: 'Save' })).toBeVisible();
  await expect(cleanupRow.getByRole('button', { name: 'Cancel' })).toBeVisible();

  await cleanupRows.getByRole('button', { name: 'Details' }).click();
  await expect(docs).toBeVisible();
  await expect(cleanupRow.getByRole('textbox')).toBeVisible();

  await cleanupRow.getByRole('button', { name: 'Cancel' }).click();
  await expect(cleanupRow.getByRole('textbox')).toHaveCount(0);
  await expect(docs).toBeVisible();

  await edit.click();
  await cleanupRow.getByRole('textbox').fill('create,compact');
  await cleanupRow.getByRole('button', { name: 'Save' }).click();
  await expect(cleanupRow.getByRole('textbox')).toHaveCount(0);
  await expect(docs).toBeVisible();
});

test('Topic inline editing reports save errors while documentation is expanded', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfiguration(page);
  await mockTopicPatch(page, 500);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  const cleanupRows = configurationRow(page, cleanupProperty);
  const cleanupRow = cleanupRows.locator('tr').first();
  const docs = cleanupRows.getByTestId('config-documentation-content');

  await cleanupRow.getByRole('button', { name: 'Edit' }).click();
  await cleanupRows.getByRole('button', { name: 'Details' }).click();
  await expect(docs).toBeVisible();

  await cleanupRow.getByRole('textbox').fill('create,compact');
  await cleanupRow.getByRole('button', { name: 'Save' }).click();

  await expect(cleanupRow.getByRole('textbox')).toBeVisible();
  await expect(page.getByText(/API Error: 500/)).toBeVisible();
  await expect(docs).toBeVisible();
});
