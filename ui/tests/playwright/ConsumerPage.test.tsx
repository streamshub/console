import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Consumer  page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to consumer page", async () => {
    await expect(page.getByRole('columnheader', { name: 'Consumer group name' })).toBeVisible();
    await authenticatedPage.clickFirstLinkInTheTable("Consumer groups");
  });
  await test.step("Consumer  page should display table", async () => {
    await expect(page.getByRole('columnheader', { name: 'Member ID' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Overall lag' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Assigned partitions' })).toBeVisible();

    await page.getByRole('button', { name: 'Details' }).click();
    await expect(page.getByRole('columnheader', { name: 'Topic' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Partition', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Lag', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Committed offset' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'End offset' })).toBeVisible();
  });
});
