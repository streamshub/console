import type { Page } from '@playwright/test';
import { expect, test } from './authenticated-test';

const fallbackText = 'No documentation available for this property.';

const missingDocumentationProperty = 'fallback.missing';
const emptyDocumentationProperty = 'fallback.empty';
const whitespaceDocumentationProperty = 'fallback.whitespace';

async function mockTopicConfigurationFallbacks(page: Page) {
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
          attributes: {
            name: 'mock-topic',
            configs: {
              [missingDocumentationProperty]: {
                value: 'one',
                source: 'DEFAULT_CONFIG',
                sensitive: false,
                readOnly: false,
                type: 'STRING',
              },
              [emptyDocumentationProperty]: {
                value: 'two',
                source: 'DEFAULT_CONFIG',
                sensitive: false,
                readOnly: false,
                type: 'STRING',
                documentation: '',
              },
              [whitespaceDocumentationProperty]: {
                value: 'three',
                source: 'DEFAULT_CONFIG',
                sensitive: false,
                readOnly: false,
                type: 'STRING',
                documentation: '   \n\t ',
              },
            },
          },
        },
      }),
    });
  });
}

function configurationRows(page: Page, property: string) {
  return page
    .locator('table[data-ouia-component-id="topic-configuration-table"]')
    .locator('tbody')
    .filter({ hasText: property });
}

async function expectFallbackDocumentation(page: Page, property: string) {
  const rows = configurationRows(page, property);
  const toggle = rows.getByRole('button', { name: 'Details' });
  const documentation = rows.getByTestId('config-documentation-content');

  await expect(toggle).toBeVisible();
  await expect(documentation).toBeHidden();

  await toggle.click();
  await expect(documentation).toBeVisible();
  await expect(
    documentation.getByRole('heading', { name: 'Documentation' })
  ).toBeVisible();
  await expect(documentation).toContainText(fallbackText);
  await expect(documentation.getByText(fallbackText, { exact: true })).toBeVisible();

  const rawDocumentation = await documentation
    .locator('div')
    .last()
    .evaluate((element) => element.textContent);
  expect(rawDocumentation).toBe(fallbackText);
}

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToFirstTopic();
});

test('Missing documentation shows the translated fallback', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfigurationFallbacks(page);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  await expectFallbackDocumentation(page, missingDocumentationProperty);
});

test('Empty documentation shows the translated fallback', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfigurationFallbacks(page);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  await expectFallbackDocumentation(page, emptyDocumentationProperty);
});

test('Whitespace-only documentation shows the translated fallback', async ({
  page,
  authenticatedPage,
}) => {
  await mockTopicConfigurationFallbacks(page);
  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  await expectFallbackDocumentation(page, whitespaceDocumentationProperty);
});
