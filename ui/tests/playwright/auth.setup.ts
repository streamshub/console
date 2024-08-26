import { expect, test as setup } from "@playwright/test";

setup("authenticate", async ({ page }) => {
  await page.goto("/");
  await page.waitForURL("**/login", { waitUntil: "commit" });
  //await page.waitForURL("./", { waitUntil: "commit" });
  await expect(page.getByText("Welcome to the StreamsHub console")).toBeVisible();
  await page.click('text="Click to login anonymously"');
  await page.waitForURL("**/overview", { waitUntil: "commit" });
  const newPage = page.mainFrame();
  expect(await newPage.innerText("body")).toContain("Cluster overview");
});
