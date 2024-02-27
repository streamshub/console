import { defineConfig } from 'playwright/test';

export default defineConfig({
  // Run your local dev server before starting the tests
  webServer: {
    command: 'npm run dev',
    url: 'https://console-api-eyefloaters-dev.mycluster-us-east-107719-da779aef12eee96bd4161f4e402b70ec-0000.us-east.containers.appdomain.cloud',
    reuseExistingServer: !process.env.CI,
    stdout: 'ignore',
    stderr: 'pipe',
    timeout: 120 * 1000,
  },
  use: {
    baseURL: 'https://console-api-eyefloaters-dev.mycluster-us-east-107719-da779aef12eee96bd4161f4e402b70ec-0000.us-east.containers.appdomain.cloud',
  },
});