import type { StorybookConfig } from "@storybook/nextjs";
import TsconfigPathsPlugin from "tsconfig-paths-webpack-plugin";

const config: StorybookConfig & { chromatic: any } = {
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
  // Add configuration for Chromatic
  chromatic: {
    // Use this function to ignore specific stories or tests
    ignore: async (ctx: any) => {
      // Determine if the story or test should be ignored
      if (
        Array.isArray(ctx.story.tags) &&
        ctx.story.tags.includes("skip-test")
      ) {
        // Return true to ignore the story or test
        return true;
      }
      // Return false to include the story or test
      return false;
    },
  },
};
export default config;
