import { authFile } from "./playwright.config";
import { expect, test as setup } from "@playwright/test";

setup("authenticate", async ({ page }) => {
  await page.goto("./");
  await page.waitForURL("**/login", { waitUntil: "commit" });
  await page.click('text="Click to login anonymously"');
  await page.waitForURL("**/overview", { waitUntil: "commit" });
  await expect(
    page.getByRole("heading", { name: "Cluster overview" }),
  ).toBeVisible();
  const newPage = page.mainFrame();
  expect(await newPage.innerText("body")).toContain("Cluster overview");

  process.env.TEST_BASE_URL = page.url();

  await page.context().storageState({ path: authFile });
});
