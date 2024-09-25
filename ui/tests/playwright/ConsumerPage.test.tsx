import { expect, test } from "./authenticated-test";

test.beforeEach(async ({ authenticatedPage }) => {
  await authenticatedPage.goToConsumerGroups();
});

test("Consumer  page", async ({ page, authenticatedPage }) => {
  await test.step("Navigate to consumer page", async () => {
    await authenticatedPage.clickFirstLinkInTheTable("Consumer groups");
    await authenticatedPage.waitForTableLoaded();
  });
  await test.step("Consumer  page should display table", async () => {
    expect(await page.innerText("body")).toContain("Member ID");
    expect(await page.innerText("body")).toContain("Overall lag");
    expect(await page.innerText("body")).toContain("Assigned partitions");
    const button = page
      .locator(
        'button[aria-labelledby="simple-node0 00"][aria-label="Details"]',
      )
      .first();
    await button?.click();
    expect(await page.innerText("body")).toContain("Committed offset");
    expect(await page.innerText("body")).toContain("Topic");
    expect(await page.innerText("body")).toContain("Partition");
    expect(await page.innerText("body")).toContain("Lag");
    expect(await page.innerText("body")).toContain("End offset");
  });
});
