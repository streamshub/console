import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToKafkaUser();
});

test("Kafka Users page: shows table headers or empty state", async ({
  page,
}) => {
  await test.step("Page loaded", async () => {
    await expect(
      page.getByRole("heading", { name: "Kafka Users" }),
    ).toBeVisible();
  });

  // Check empty state
  const emptyState = page.getByText("No kafka users");
  if (await emptyState.isVisible()) {
    await expect(emptyState).toBeVisible();
    return;
  }

  await test.step("Table is visible", async () => {
    await expect(page.getByLabel("kafka user table")).toBeVisible();
  });

  await test.step("Verify table headers", async () => {
    const headerRows = await page
      .locator('table[aria-label="kafka user table"] thead tr')
      .all();

    const headerRow = headerRows[0]; // First row of the header

    expect(await headerRow.locator("th").nth(0).innerText()).toBe("Name");
    expect(await headerRow.locator("th").nth(1).innerText()).toBe("Namespace");
    expect(await headerRow.locator("th").nth(2).innerText()).toBe(
      "Creation Time",
    );
    expect(await headerRow.locator("th").nth(3).innerText()).toBe("Username");
    expect(await headerRow.locator("th").nth(4).innerText()).toBe(
      "Authentication",
    );
  });

  await test.step("Verify table rows exist", async () => {
    const rows = page.locator('table[aria-label="kafka user table"] tbody tr');
    expect(await rows.count()).toBeGreaterThan(0);

    const dataCells = await page
      .locator('table[aria-label="kafka user table"] tbody tr td')
      .evaluateAll((tds) => tds.map((td) => td.innerHTML?.trim() ?? ""));

    expect(dataCells.length).toBeGreaterThan(0);
  });
});
