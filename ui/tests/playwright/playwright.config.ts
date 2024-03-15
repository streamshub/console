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
      timeout: 15000,
    },

    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        // Use prepared auth state.
        storageState: "tests/playwright/.auth/user.json",
      },
      dependencies: ["setup"],
      timeout: 15000,
    },
  ],
  // Run your local dev server before starting the tests
  webServer: {
    command: "npm run start",
    url: "http://localhost:3000",
    env: {
      NEXTAUTH_SECRET: process.env.NEXTAUTH_SECRET,
      BACKEND_URL: process.env.BACKEND_URL,
      LOG_LEVEL: process.env.LOG_LEVEL,
      CONSOLE_MODE: process.env.CONSOLE_MODE,
      CONSOLE_METRICS_PROMETHEUS_URL:
        process.env.CONSOLE_METRICS_PROMETHEUS_URL,
    },
    reuseExistingServer: !process.env.CI,
    stdout: "pipe",
    stderr: "pipe",
    timeout: 15000,
  },
  use: {
    baseURL: " http://localhost:3000",
  },
});
