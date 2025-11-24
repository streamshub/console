import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Consumer page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to consumer page or show empty state", async () => {
    const tableRows = page.locator(
      'table[aria-label="Consumer groups"] tbody tr'
    );
    const emptyState = page.getByText("No consumer groups");

    // Wait for either a row OR empty state
    await Promise.race([
      tableRows.first().waitFor({ state: "visible" }).catch(() => null),
      emptyState.waitFor({ state: "visible" }).catch(() => null)
    ]);

    // ----- EMPTY STATE -----
    if (await emptyState.isVisible()) {
      await expect(emptyState).toBeVisible();
      test.skip(true, "No consumer groups exist, skipping navigation to consumer page");
      return;
    }

    // ----- TABLE EXISTS -----
    await expect(page.getByRole("columnheader", { name: "Consumer group name" })).toBeVisible();

    await authenticatedPage.clickFirstLinkInTheTable("Consumer groups");
  });

  await test.step("Consumer page should display table or empty state", async () => {
    const memberRows = page.locator('table[aria-label="Consumer"] tbody tr');
    const emptyState = page.getByText("No consumer");

    await Promise.race([
      memberRows.first().waitFor({ state: "visible" }).catch(() => null),
      emptyState.waitFor({ state: "visible" }).catch(() => null)
    ]);

    // ----- EMPTY STATE -----
    if (await emptyState.isVisible()) {
      await expect(emptyState).toBeVisible();
      return;
    }

    // ----- TABLE EXISTS -----
    await expect(page.getByRole("columnheader", { name: "Member ID" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Overall lag" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Assigned partitions" })).toBeVisible();

    await page.getByRole("button", { name: "Details" }).click();
    await expect(page.getByRole("columnheader", { name: "Topic" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Partition", exact: true })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Lag", exact: true })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Committed offset" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "End offset" })).toBeVisible();
  });
});
