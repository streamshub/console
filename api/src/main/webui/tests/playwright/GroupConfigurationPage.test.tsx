import type { Page } from "@playwright/test";
import { expect, test } from "./authenticated-test";
import type { AuthenticatedPage } from "./authenticated-page";

const resetProperty = "auto.offset.reset";
const timeoutProperty = "session.timeout.ms";
const resetDocumentation = "  Auto offset reset documentation.\n  ";
const timeoutDocumentation = "Session timeout documentation.";

async function mockGroupConfiguration(page: Page) {
  await page.route(/\/api\/kafkas\/[^/]+\/groups\/[^/]+\?/, async (route) => {
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
          id: "mock-group",
          type: "groups",
          attributes: {
            groupId: "mock-group",
            state: "STABLE",
            protocol: "consumer",
            configs: {
              [resetProperty]: {
                value: "latest",
                source: "DEFAULT_CONFIG",
                sensitive: false,
                readOnly: false,
                type: "STRING",
                documentation: resetDocumentation,
              },
              [timeoutProperty]: {
                value: "45000",
                source: "PER_GROUP_CONFIG",
                sensitive: false,
                readOnly: true,
                type: "STRING",
                documentation: timeoutDocumentation,
              },
            },
          },
        },
      }),
    });
  });
}

async function navigateToGroupConfiguration(page: Page, authenticatedPage: AuthenticatedPage) {
  await authenticatedPage.goToGroups();

  if (await page.getByText("No groups").isVisible()) {
    test.skip(true, "No consumer groups available in this environment");
  }

  await authenticatedPage.clickFirstLinkInTheTable("kafka-groups-table");
  await authenticatedPage.clickTab("Configuration");
  await expect(page.getByRole("columnheader", { name: "Property" })).toBeVisible();
}

function groupConfigurationTable(page: Page) {
  return page.getByRole("grid", { name: "Group configuration table" });
}

function groupConfigurationRows(page: Page, property: string) {
  return groupConfigurationTable(page).locator("tbody").filter({ hasText: property });
}

test("Group configuration expands documentation beneath the matching property row", async ({
  page,
  authenticatedPage,
}) => {
  await mockGroupConfiguration(page);
  await navigateToGroupConfiguration(page, authenticatedPage);

  await test.step("Sortable headers expose correct sort metadata", async () => {
    await expect(page.getByRole("columnheader", { name: "Property" })).toHaveAttribute("aria-sort", "ascending");
    await expect(page.getByRole("columnheader", { name: "Source" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Value" })).not.toHaveAttribute("aria-sort");
  });

  const resetRows = groupConfigurationRows(page, resetProperty);
  const timeoutRows = groupConfigurationRows(page, timeoutProperty);
  const resetToggle = resetRows.getByRole("button", { name: "Details" });
  const timeoutToggle = timeoutRows.getByRole("button", { name: "Details" });
  const resetDocs = resetRows.getByTestId("config-documentation-content");
  const timeoutDocs = timeoutRows.getByTestId("config-documentation-content");

  await test.step("Show an accessible expander for each configuration property", async () => {
    await expect(resetToggle).toBeVisible();
    await expect(timeoutToggle).toBeVisible();
    await expect(resetDocs).toBeHidden();
  });

  await test.step("Expand the correct row with the property's documentation", async () => {
    await resetToggle.click();
    await expect(resetDocs).toBeVisible();
    await expect(resetDocs.locator("xpath=ancestor::td")).toHaveAttribute("colspan", "4");
    await expect(resetDocs.getByRole("heading", { name: "Documentation" })).toBeVisible();
    await expect(resetDocs).toContainText(resetDocumentation.trim());

    const rawDocumentation = await resetDocs
      .locator("div")
      .last()
      .evaluate((element) => element.textContent);
    expect(rawDocumentation).toBe(resetDocumentation.trim());

    await expect(timeoutDocs).toBeHidden();
  });

  await test.step("Expand another row without mixing documentation", async () => {
    await timeoutToggle.click();
    await expect(timeoutDocs).toBeVisible();
    await expect(timeoutDocs).toContainText(timeoutDocumentation);
    await expect(resetDocs).not.toContainText(timeoutDocumentation);
  });

  await test.step("Collapse the documentation again", async () => {
    await resetToggle.click();
    await expect(resetDocs).toBeHidden();

    await timeoutToggle.click();
    await expect(timeoutDocs).toBeHidden();
  });
});

test("Group configuration preserves property details while documentation is expanded", async ({
  page,
  authenticatedPage,
}) => {
  await mockGroupConfiguration(page);
  await navigateToGroupConfiguration(page, authenticatedPage);

  const resetRows = groupConfigurationRows(page, resetProperty);
  const timeoutRows = groupConfigurationRows(page, timeoutProperty);
  const resetRow = resetRows.locator("tr").first();
  const timeoutRow = timeoutRows.locator("tr").first();
  const resetToggle = resetRows.getByRole("button", { name: "Details" });
  const resetDocs = resetRows.getByTestId("config-documentation-content");

  const resetName = resetRow.getByText(resetProperty, { exact: true });
  const resetValue = resetRow.getByText("latest", { exact: true });
  const resetSource = resetRow.getByRole("gridcell", { name: "DEFAULT_CONFIG", exact: true });
  const timeoutName = timeoutRow.getByText(timeoutProperty, { exact: true });
  const timeoutValue = timeoutRow.getByText("45000", { exact: true });
  const timeoutSource = timeoutRow.getByRole("gridcell", { name: "PER_GROUP_CONFIG", exact: true });
  const timeoutReadOnlyLabel = timeoutRow.getByText("Read-only", { exact: true });

  await expect(resetName).toBeVisible();
  await expect(resetValue).toBeVisible();
  await expect(resetSource).toBeVisible();
  await expect(timeoutName).toBeVisible();
  await expect(timeoutValue).toBeVisible();
  await expect(timeoutSource).toBeVisible();
  await expect(timeoutReadOnlyLabel).toBeVisible();

  const beforeExpansion = await resetRow.innerText();
  const beforeOtherRowExpansion = await timeoutRow.innerText();

  await resetToggle.click();
  await expect(resetDocs).toBeVisible();

  await expect(resetName).toBeVisible();
  await expect(resetValue).toBeVisible();
  await expect(resetSource).toBeVisible();
  await expect(timeoutName).toBeVisible();
  await expect(timeoutValue).toBeVisible();
  await expect(timeoutSource).toBeVisible();
  await expect(timeoutReadOnlyLabel).toBeVisible();
  expect(await resetRow.innerText()).toBe(beforeExpansion);
  expect(await timeoutRow.innerText()).toBe(beforeOtherRowExpansion);
});
