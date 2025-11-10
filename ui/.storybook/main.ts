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

      // This is a workaround to a bug in storybook that prevents storybook v10.x.x working with Next.js v14.x.x
      // See: https://github.com/storybookjs/storybook/issues/32950
      //
      // This can be removed when we upgrade to Next.js v15.0.0+ or if storybook releases a fix in a newer
      // version.
      config.resolve.alias = {
          ...(config.resolve.alias || {}),
          'next/dist/server/request/draft-mode': false,
      };

      return config;
  }
};
export default config;
