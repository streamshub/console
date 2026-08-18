import { mergeConfig } from 'vitest/config';
import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';
import { playwright } from '@vitest/browser-playwright';
import viteConfig from './vite.config.ts';

export default mergeConfig(viteConfig, {
  plugins: [
    storybookTest(),
  ],
  // Bundle CJS-only packages imported by @storybook/addon-vitest's setup file.
  // Scoped to the test environment — has no effect on the production build.
  optimizeDeps: {
    include: [
      'aria-query',
      'lz-string',
      'pretty-format',
    ],
  },
  ssr: {
    noExternal: true,
  },
  test: {
    name: 'storybook',
    browser: {
      enabled: true,
      headless: true,
      provider: playwright({}),
      instances: [{ browser: 'chromium' }],
    },
  },
});
