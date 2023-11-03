import type { StorybookConfig } from "@storybook/nextjs";

const config: StorybookConfig = {

  stories: ['../stories/**/*.stories.@(js|jsx|ts|tsx)'],
  addons: [
    "@storybook/addon-links",
    "@storybook/addon-essentials",
    "@storybook/addon-onboarding",
    "@storybook/addon-interactions",
  ],
  framework: {
    name: "@storybook/nextjs",
    options: {},
  },
  docs: {
  },
};
export default config;
