import NextIntlProvider from "@/app/[locale]/NextIntlProvider";
import { Page } from "@patternfly/react-core";
import type { Preview } from "@storybook/nextjs";
import "../app/globals.css";
import messages from "../messages/en.json";

const preview: Preview = {
  globalTypes: {
    theme: {
      name: "Theme",
      description: "Global theme for components",
      defaultValue: "light",
      toolbar: {
        icon: "paintbrush",
        items: [
          { value: "light", title: "Light Theme" },
          { value: "dark", title: "Dark Theme" },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
  },

  parameters: {
    nextjs: {
      appDirectory: true,
    },
    actions: { argTypesRegex: "^on[A-Z].*" },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    layout: "fullscreen",
  },

  decorators: [
    (Story, context) => {
      const theme = context.globals.theme;

      // Remove both classes first
      document.documentElement.classList.remove(
        "pf-v6-theme-dark",
        "pf-v6-theme-light",
      );

      // Apply based on the theme selected
      document.documentElement.classList.add(
        theme === "dark" ? "pf-v6-theme-dark" : "pf-v6-theme-light",
      );

      return (
        <NextIntlProvider locale={"en"} messages={messages}>
          <Page>
            <Story />
          </Page>
        </NextIntlProvider>
      );
    },
  ],
};

export default preview;
