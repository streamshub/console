import { defineConfig, devices } from "playwright/test";

export default defineConfig({
  // Retry on CI only.
  retries: process.env.CI_CLUSTER ? 5 : 0,

  // Opt out of parallel tests on CI.
  workers: process.env.CI_CLUSTER ? 1 : undefined,

  projects: [
    //{ name: "setup", testMatch: /.*\.setup\.ts/ },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        // Use prepared auth state.
        // storageState: "tests/playwright/.auth/user.json",
      },
      //dependencies: ["setup"],
      timeout: 25000,
    },

    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        // Use prepared auth state.
        // storageState: "tests/playwright/.auth/user.json",
      },
      //dependencies: ["setup"],
      timeout: 25000,
    },
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL,
    ignoreHTTPSErrors: true,
  },
});
