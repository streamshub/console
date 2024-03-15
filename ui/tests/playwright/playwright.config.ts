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
    url: "http://localhost:3000",
    env: {
      NEXTAUTH_SECRET: "SvZMwFKn7myfCwQwDEvWgG3S7iLX32VMyLbTDxM1Oyg=",
      BACKEND_URL:
        "https://console-api-eyefloaters-dev.mycluster-us-east-107719-da779aef12eee96bd4161f4e402b70ec-0000.us-east.containers.appdomain.cloud",
      LOG_LEVEL: "info",
      CONSOLE_MODE: "read-only",
      CONSOLE_METRICS_PROMETHEUS_URL: "https://foo",
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
