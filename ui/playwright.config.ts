import { defineConfig, devices } from "playwright/test";

export default defineConfig({
  projects: [
    { name: "setup", testMatch: /.*\.setup\.ts/ },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        // Use prepared auth state.
        storageState: "tests/playwright/.auth/user.json",
      },
      dependencies: ["setup"],
      timeout: 120000,
    },

    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        // Use prepared auth state.
        storageState: "tests/playwright/.auth/user.json",
      },
      dependencies: ["setup"],
      timeout: 60000,
    },
  ],
  // Run your local dev server before starting the tests
  webServer: {
    command: "npm run start",
    url: " http://127.0.0.1:3000",
    reuseExistingServer: !process.env.CI,
    stdout: "ignore",
    stderr: "pipe",
    timeout: 120 * 1000,
  },
  use: {
    baseURL: " http://localhost:3000",  
  },
});
