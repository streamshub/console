import type { Page } from "@playwright/test";
import { expect, test } from "./authenticated-test";
import type { AuthenticatedPage } from "./authenticated-page";

const rackProperty = "broker.rack";
const logDirsProperty = "log.dirs";
const rackDocumentation = "  Broker rack documentation.\n  ";
const logDirsDocumentation = "Log directories documentation.";

async function mockNodeConfiguration(page: Page) {
  await page.route(/\/api\/kafkas\/[^/]+\/nodes\/[^/]+\/configs\?/, async (route) => {
    const url = new URL(route.request().url());

    if (!url.searchParams.has("fields[configs]")) {
      await route.continue();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          id: "mock-node",
          type: "nodes",
          attributes: {
            [rackProperty]: {
              value: "rack-a",
              source: "PER_BROKER_CONFIG",
              sensitive: false,
              readOnly: true,
              type: "STRING",
              documentation: rackDocumentation,
            },
            [logDirsProperty]: {
              value: "/var/lib/kafka/logs",
              source: "DEFAULT_CONFIG",
              sensitive: false,
              readOnly: false,
              type: "STRING",
              documentation: logDirsDocumentation,
            },
          },
        },
      }),
    });
  });
}

async function navigateToNodeConfiguration(page: Page, authenticatedPage: AuthenticatedPage) {
  await page.click('text="Kafka Nodes"');
  await expect(page.getByRole("columnheader", { name: "Node ID" })).toBeVisible();
  await authenticatedPage.clickFirstLinkInTheTable("nodes-table");
  await expect(page.getByRole("columnheader", { name: "Property" })).toBeVisible();
}

function nodeConfigurationRows(page: Page, property: string) {
  return page
    .locator('table[data-ouia-component-id="node-configuration-listing"]')
    .locator("tbody")
    .filter({ hasText: property });
}

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToClusterOverview();
});

test("Node property page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to node property page", async () => {
    await page.click('text="Kafka Nodes"');
    await expect(page.getByRole('columnheader', { name: 'Node ID' })).toBeVisible();
    await authenticatedPage.clickFirstLinkInTheTable("nodes-table");
    await expect(page.getByRole('columnheader', { name: 'Property' })).toBeVisible();
  });
  await test.step("Sortable headers expose correct sort metadata", async () => {
    await expect(page.getByRole('columnheader', { name: 'Property' })).toHaveAttribute('aria-sort', 'ascending');
    await expect(page.getByRole('columnheader', { name: 'Source' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Value' })).not.toHaveAttribute('aria-sort');
  });
  await test.step("Node page should display properties", async () => {
    await page.waitForFunction(() => {
      return (
        document.querySelectorAll(
          'table[data-ouia-component-id="node-configuration-listing"] tbody tr',
        ).length > 0
      );
    });

    const dataCells = await page
      .locator('table[data-ouia-component-id="node-configuration-listing"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});

test("Node configuration expands documentation beneath the matching property row", async ({
  page,
  authenticatedPage,
}) => {
  await mockNodeConfiguration(page);
  await navigateToNodeConfiguration(page, authenticatedPage);

  const rackRows = nodeConfigurationRows(page, rackProperty);
  const logDirsRows = nodeConfigurationRows(page, logDirsProperty);
  const rackToggle = rackRows.getByRole("button", { name: "Details" });
  const logDirsToggle = logDirsRows.getByRole("button", { name: "Details" });
  const rackDocs = rackRows.getByTestId("config-documentation-content");
  const logDirsDocs = logDirsRows.getByTestId("config-documentation-content");

  await test.step("Show an accessible expander for the configuration property", async () => {
    await expect(rackToggle).toBeVisible();
    await expect(rackDocs).toBeHidden();
  });

  await test.step("Expand the correct row with the property's documentation", async () => {
    await rackToggle.click();
    await expect(rackDocs).toBeVisible();
    await expect(rackDocs.locator("xpath=ancestor::td")).toHaveAttribute("colspan", "4");
    await expect(rackDocs.getByRole("heading", { name: "Documentation" })).toBeVisible();
    await expect(rackDocs).toContainText(rackDocumentation.trim());

    const rawDocumentation = await rackDocs
      .locator("div")
      .last()
      .evaluate((element) => element.textContent);
    expect(rawDocumentation).toBe(rackDocumentation.trim());

    await expect(logDirsDocs).toBeHidden();
  });

  await test.step("Expand another row without mixing documentation", async () => {
    await logDirsToggle.click();
    await expect(logDirsDocs).toBeVisible();
    await expect(logDirsDocs).toContainText(logDirsDocumentation);
    await expect(rackDocs).not.toContainText(logDirsDocumentation);
  });

  await test.step("Collapse the documentation again", async () => {
    await rackToggle.click();
    await expect(rackDocs).toBeHidden();

    await logDirsToggle.click();
    await expect(logDirsDocs).toBeHidden();
  });
});

test("Node configuration preserves property details while documentation is expanded", async ({
  page,
  authenticatedPage,
}) => {
  await mockNodeConfiguration(page);
  await navigateToNodeConfiguration(page, authenticatedPage);

  const rackRows = nodeConfigurationRows(page, rackProperty);
  const rackRow = rackRows.locator("tr").first();
  const rackToggle = rackRows.getByRole("button", { name: "Details" });
  const rackDocs = rackRows.getByTestId("config-documentation-content");

  const propertyName = rackRow.getByText(rackProperty, { exact: true });
  const propertyValue = rackRow.getByText("rack-a", { exact: true });
  const sourceCell = rackRow.getByRole("gridcell", { name: "PER_BROKER_CONFIG", exact: true });
  const readOnlyLabel = rackRow.getByText("Read-only", { exact: true });

  await expect(propertyName).toBeVisible();
  await expect(propertyValue).toBeVisible();
  await expect(sourceCell).toBeVisible();
  await expect(readOnlyLabel).toBeVisible();

  const beforeExpansion = await rackRow.innerText();

  await rackToggle.click();
  await expect(rackDocs).toBeVisible();

  await expect(propertyName).toBeVisible();
  await expect(propertyValue).toBeVisible();
  await expect(sourceCell).toBeVisible();
  await expect(readOnlyLabel).toBeVisible();
  expect(await rackRow.innerText()).toBe(beforeExpansion);
});
