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
    "@storybook/addon-docs"
  ],

  features: {
    experimentalRSC: true,
  },

  framework: {
    name: "@storybook/nextjs",
    options: {},
  },

  webpackFinal: async (config) => {
      config.resolve = config.resolve || {};

      config.resolve.plugins = [
          ...(config.resolve.plugins || []),
          new TsconfigPathsPlugin(),
      ];
      return config;
  }
};
export default config;
