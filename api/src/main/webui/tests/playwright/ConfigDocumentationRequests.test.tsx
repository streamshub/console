import type { Page } from '@playwright/test';
import { CONFIG_DOCUMENTATION_FIELDS } from '../../src/api/configFields';
import { expect, test } from './authenticated-test';

const topicDetailPath = /^\/api\/kafkas\/[^/]+\/topics\/[^/]+$/;
const groupDetailPath = /^\/api\/kafkas\/[^/]+\/groups\/[^/]+$/;
const nodeConfigPath = /^\/api\/kafkas\/[^/]+\/nodes\/[^/]+\/configs$/;

function trackApiRequests(page: Page): URL[] {
  const requests: URL[] = [];

  page.on('request', (request) => {
    const url = new URL(request.url());
    if (url.pathname.startsWith('/api/kafkas/')) {
      requests.push(url);
    }
  });

  return requests;
}

function expectNoDocumentationRequests(requests: URL[]) {
  for (const url of requests) {
    expect(url.searchParams.get('fields[configs]')).toBeNull();
  }
}

test('Topic list requests do not include config documentation', async ({
  page,
  authenticatedPage,
}) => {
  const requests = trackApiRequests(page);

  await test.step('Open the topics list', async () => {
    await authenticatedPage.goToTopics();
    await expect(
      page.getByRole('columnheader', { name: 'Name' })
    ).toBeVisible();
  });

  await test.step('Verify list requests omit config documentation', async () => {
    const listRequests = requests.filter((url) =>
      url.pathname.endsWith('/topics')
    );

    expect(listRequests.length).toBeGreaterThan(0);
    expectNoDocumentationRequests(listRequests);
  });
});

test('Topic configuration requests the complete documentation field list', async ({
  page,
  authenticatedPage,
}) => {
  await authenticatedPage.goToFirstTopic();

  const documentationRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return (
      topicDetailPath.test(url.pathname) &&
      url.searchParams.has('fields[configs]')
    );
  });

  await authenticatedPage.clickTab('Configuration');

  const params = new URL((await documentationRequest).url()).searchParams;
  expect(params.get('fields[configs]')).toBe(CONFIG_DOCUMENTATION_FIELDS);

  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();
});

test('Group list requests do not include config documentation', async ({
  page,
  authenticatedPage,
}) => {
  const requests = trackApiRequests(page);

  await test.step('Open the groups list', async () => {
    await authenticatedPage.goToGroups();
    await expect(
      page.getByText('No groups').or(page.getByRole('columnheader', { name: 'Group ID' }))
    ).toBeVisible();
  });

  await test.step('Verify list requests omit config documentation', async () => {
    const listRequests = requests.filter((url) =>
      url.pathname.endsWith('/groups')
    );

    expect(listRequests.length).toBeGreaterThan(0);
    expectNoDocumentationRequests(listRequests);
  });
});

test('Group detail requests documentation once with the complete field list', async ({
  page,
  authenticatedPage,
}) => {
  const requests = trackApiRequests(page);

  await authenticatedPage.goToGroups();

  if (await page.getByText('No groups').isVisible()) {
    test.skip(true, 'No consumer groups available in this environment');
  }

  const documentationRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return (
      groupDetailPath.test(url.pathname) &&
      url.searchParams.has('fields[configs]')
    );
  });

  await authenticatedPage.clickFirstLinkInTheTable('kafka-groups-table');

  const params = new URL((await documentationRequest).url()).searchParams;
  expect(params.get('fields[configs]')).toBe(CONFIG_DOCUMENTATION_FIELDS);

  await authenticatedPage.clickTab('Configuration');
  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  const detailDocumentationRequests = requests.filter(
    (url) =>
      groupDetailPath.test(url.pathname) &&
      url.searchParams.has('fields[configs]')
  );

  expect(detailDocumentationRequests).toHaveLength(1);
});

test('Node list requests do not include config documentation', async ({
  page,
  authenticatedPage,
}) => {
  const requests = trackApiRequests(page);

  await test.step('Open the nodes list', async () => {
    await authenticatedPage.goToClusterOverview();
    await page.click('text="Kafka Nodes"');
    await expect(
      page.getByRole('columnheader', { name: 'Node ID' })
    ).toBeVisible();
  });

  await test.step('Verify list requests omit config documentation', async () => {
    const listRequests = requests.filter((url) =>
      url.pathname.endsWith('/nodes')
    );

    expect(listRequests.length).toBeGreaterThan(0);
    expectNoDocumentationRequests(listRequests);
  });
});

test('Node detail requests documentation once with the complete field list', async ({
  page,
  authenticatedPage,
}) => {
  const requests = trackApiRequests(page);

  await authenticatedPage.goToClusterOverview();
  await page.click('text="Kafka Nodes"');
  await expect(
    page.getByRole('columnheader', { name: 'Node ID' })
  ).toBeVisible();

  const documentationRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return (
      nodeConfigPath.test(url.pathname) &&
      url.searchParams.has('fields[configs]')
    );
  });

  await authenticatedPage.clickFirstLinkInTheTable('nodes-table');

  const params = new URL((await documentationRequest).url()).searchParams;
  expect(params.get('fields[configs]')).toBe(CONFIG_DOCUMENTATION_FIELDS);

  await expect(
    page.getByRole('columnheader', { name: 'Property' })
  ).toBeVisible();

  const detailDocumentationRequests = requests.filter(
    (url) =>
      nodeConfigPath.test(url.pathname) &&
      url.searchParams.has('fields[configs]')
  );

  expect(detailDocumentationRequests).toHaveLength(1);
});
