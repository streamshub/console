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

/** A records response in the form of the server-sent events the records endpoint produces. */
function recordsStream(done: string, heartbeat: object) {
  const record = {
    id: '0:1',
    type: 'records',
    attributes: {
      partition: 0,
      offset: 1,
      timestamp: '2026-10-28T10:15:30.123Z',
      key: 'mock-key',
      value: 'mock-needle-value',
      headers: {},
      size: 17,
    },
    relationships: {},
    meta: {},
  };

  return [
    `data: ${JSON.stringify([record])}\n\n`,
    `event: heartbeat\ndata: ${JSON.stringify(heartbeat)}\n\n`,
    `event: done\ndata: ${JSON.stringify({ reason: done })}\n\n`,
  ].join('');
}

test.describe('Messages status with a mocked records stream', () => {
  const mockTopicId = 'mock-topic-id';
  let messagesUrl: string;

  test.beforeEach(async ({ page, authenticatedPage }) => {
    // The topic itself is mocked too, so these tests do not depend on the topics in the cluster
    await page.route(
      new RegExp(`/api/kafkas/[^/]+/topics/${mockTopicId}\\?`),
      (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              id: mockTopicId,
              type: 'topics',
              attributes: {
                name: 'mock-topic',
                numPartitions: 1,
                status: 'FullyReplicated',
                visibility: 'external',
              },
              meta: {},
            },
          }),
        })
    );

    // Only the stream of records, not the request for a single record (/records/<partition>/<offset>)
    await page.route(new RegExp(`/topics/${mockTopicId}/records\\?`), async (route) => {
      const url = new URL(route.request().url());
      const searching = url.searchParams.has('filter[value]');

      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        body: recordsStream(searching ? 'timeout' : 'complete', {
          scanned: searching ? 42 : 1,
          progress: 100,
          elapsed: searching ? 3400 : 1200,
          remaining: searching ? 296600 : 298800,
        }),
      });
    });

    // Find the id of the cluster to build the URL of the mocked topic
    await authenticatedPage.goToClusterOverview();
    const kafkaId = new URL(page.url()).pathname.split('/')[2];
    messagesUrl = `/kafka/${kafkaId}/topics/${mockTopicId}/messages`;
  });

  test('Shows how long it took after retrieving without a search term', async ({
    page,
  }) => {
    await page.goto(messagesUrl);

    const status = page.getByTestId('messages-status');

    await expect(status.getByText('1 message in 0:01')).toBeVisible();
    // The end reason is only shown for a search
    await expect(status.getByText('Scan complete')).toBeHidden();
  });

  test('Shows how long the search took and why it ended', async ({ page }) => {
    await page.goto(`${messagesUrl}?from=offset&offset=0&query=needle`);

    const status = page.getByTestId('messages-status');

    await expect(
      status.getByText('1 match from 42 scanned in 0:03')
    ).toBeVisible();
    await expect(status.getByText('Timed out, partial results')).toBeVisible();

    // The time left is only of interest while the search is running
    await expect(status.getByText(/elapsed, .* left/)).toBeHidden();
  });

  test('Sends the search term with commas escaped and wildcards intact', async ({
    page,
  }) => {
    const request = page.waitForRequest(
      (r) =>
        r.url().includes(`/topics/${mockTopicId}/records?`) &&
        new URL(r.url()).searchParams.has('filter[value]')
    );

    await page.goto(
      `${messagesUrl}?from=offset&offset=0&where=value&query=${encodeURIComponent('a,b*c?d\\e')}`
    );

    const params = new URL((await request).url()).searchParams;

    // An unescaped comma would be read by the server as a second operand and rejected
    expect(params.get('filter[value]')).toBe('ilike,*a\\,b*c?d\\\\e*');

    await expect(
      page.getByTestId('messages-status').getByText('1 match from 42 scanned in 0:03')
    ).toBeVisible();
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
