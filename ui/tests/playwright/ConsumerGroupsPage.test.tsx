import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Groups page: Check for Table or Empty State", async ({
  page,
}) => {
  const emptyState = page.getByText("No groups");
  const tableHeader = page.getByRole("columnheader", {
    name: "Group ID",
  });

  await test.step("Ensure either table or empty state is visible", async () => {
    await expect(emptyState.or(tableHeader)).toBeVisible();

    if (await emptyState.isVisible()) {
      await expect(emptyState).toBeVisible();
      console.log("Empty state detected: 'No groups' is visible.");
      return;
    }
    console.log("Groups table detected. Verifying headers and data.");

    await expect(tableHeader).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "Type" }),
    ).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "Protocol" }),
    ).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "State" }),
    ).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "Overall lag" }),
    ).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "Members" }),
    ).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "Topics" }),
    ).toBeVisible();

    const dataCells = await page
      .locator('table[aria-label="Groups"] tbody tr td')
      .evaluateAll(tds => tds.map(td => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
