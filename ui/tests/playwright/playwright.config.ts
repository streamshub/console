import { defineConfig, devices } from "playwright/test";

export default defineConfig({
  projects: [
    { name: "setup", testMatch: /.*\.setup\.ts/ },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        // Use prepared auth state.
        // storageState: "tests/playwright/.auth/user.json",
      },
      dependencies: ["setup"],
      timeout: 15000,
    },

    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        // Use prepared auth state.
        // storageState: "tests/playwright/.auth/user.json",
      },
      dependencies: ["setup"],
      timeout: 15000,
    },
  ],
  use: {
    baseURL: "http://example-console.192.168.49.2.nip.io",
  },
});
