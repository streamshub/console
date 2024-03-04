import type { StorybookConfig } from "@storybook/nextjs";
import TsconfigPathsPlugin from "tsconfig-paths-webpack-plugin";

const config: StorybookConfig = {
  stories: [
    {
      directory: "../components",
      titlePrefix: "Components",
    },
    {
      directory: "../app",
      titlePrefix: "Pages",
    },
  ],
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
    autodocs: "tag",
  },
  webpackFinal: async (config, { configType }) => {
    // @ts-ignore
    config.resolve.plugins = [new TsconfigPathsPlugin()];
    return config;
  },
};
export default config;
