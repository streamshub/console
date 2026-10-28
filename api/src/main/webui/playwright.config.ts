import { defineConfig, devices } from '@playwright/test';

export const authFile = 'playwright/.auth/user.json';

const baseURL =
  process.env.PLAYWRIGHT_BASE_URL ??
  process.env.BASE_URL ??
  'http://localhost:8080';

export default defineConfig({
  timeout: 60_000, // per test (default is 30s)
  expect: { timeout: 15_000 },
  testDir: './tests/playwright',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'list' : 'html',

  use: {
    baseURL,
    ignoreHTTPSErrors: true,
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Add storageState only if these tests need a saved login.
        // storageState: authFile,
      },
    },
  ],
});
