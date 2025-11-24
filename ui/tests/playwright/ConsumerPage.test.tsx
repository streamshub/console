import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Consumer page content check", async ({ page, authenticatedPage }) => {
  const emptyStateMessage = page.getByText("No consumer groups");
  const isNoGroupsVisible = await emptyStateMessage.isVisible();

  if (isNoGroupsVisible) {
    console.log(
      "Empty state detected: 'No consumer groups' is visible. Skipping table checks.",
    );
    await expect(emptyStateMessage).toBeVisible();
  } else {
    console.log(
      "Consumer groups table expected. Proceeding with table content checks.",
    );

    await test.step("Navigate to consumer page", async () => {
      await expect(
        page.getByRole("columnheader", { name: "Consumer group name" }),
      ).toBeVisible();
      await authenticatedPage.clickFirstLinkInTheTable("Consumer groups");
    });

    await test.step("Consumer page should display details table", async () => {
      await expect(
        page.getByRole("columnheader", { name: "Member ID" }),
      ).toBeVisible();
      await expect(
        page.getByRole("columnheader", { name: "Overall lag" }),
      ).toBeVisible();
      await expect(
        page.getByRole("columnheader", { name: "Assigned partitions" }),
      ).toBeVisible();

      await page.getByRole("button", { name: "Details" }).click();

      await expect(
        page.getByRole("columnheader", { name: "Topic" }),
      ).toBeVisible();
      await expect(
        page.getByRole("columnheader", { name: "Partition", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("columnheader", { name: "Lag", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("columnheader", { name: "Committed offset" }),
      ).toBeVisible();
      await expect(
        page.getByRole("columnheader", { name: "End offset" }),
      ).toBeVisible();
    });
  }
});
