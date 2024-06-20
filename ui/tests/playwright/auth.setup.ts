import { expect, test as setup } from "@playwright/test";

// const authFile = "tests/playwright/.auth/user.json";

setup("authenticate", async ({ page }) => {
  // Perform authentication steps. Replace these actions with your own.
  await page.goto("/");
  // await page
  //   .getByRole("button", { name: "Sign in with Anonymous Session" })
  //   .click();
  // // Wait until the page receives the cookies.
  //
  // Sometimes login flow sets cookies in the process of several redirects.
  await page.waitForURL("./home", { waitUntil: "commit" });
  await expect(
    page.getByText("Welcome to the StreamsHub console"),
  ).toBeVisible();

  // await page.context().storageState({ path: authFile });
});
