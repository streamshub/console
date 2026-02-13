import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToGroups();
  await authenticatedPage.clickFirstLinkInTheTable("groups-listing", "Topics");
});

test("Topics consumers", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to topic groups page", async () => {
    await authenticatedPage.clickTab("Groups");
  });
  await test.step("Topics consumers page should display table", async () => {
    await authenticatedPage.waitForTableLoaded();
    if (await page.getByRole('heading', { name: 'No groups found' }).isVisible()) {
      return;
    }

    const groupsTable = page.locator('table[data-ouia-component-id="topic-groups-listing"]');

    expect(await groupsTable.innerText()).toContain("Group ID");
    expect(await groupsTable.innerText()).toContain("Type");
    expect(await groupsTable.innerText()).toContain("Protocol");
    expect(await groupsTable.innerText()).toContain("State");
    expect(await groupsTable.innerText()).toContain("Overall lag");
    expect(await groupsTable.innerText()).toContain("Members");
    expect(await groupsTable.innerText()).toContain("Topics");

    await groupsTable.locator("tbody").waitFor();
    const dataRows = await groupsTable.locator("tbody tr").count();
    expect(dataRows).toBeGreaterThan(0);

    const dataCells = await groupsTable.locator("tbody tr td")
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));
    expect(dataCells.length).toBeGreaterThan(0);
  });
});
