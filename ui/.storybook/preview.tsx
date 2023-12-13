import NextIntlProvider from "@/app/[locale]/NextIntlProvider";
import { Page } from "@patternfly/react-core";
import type { Preview } from "@storybook/react";
import "../app/globals.css";
import messages from "../messages/en.json";

const preview: Preview = {
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
    (Story) => (
      <NextIntlProvider locale={"en"} messages={messages}>
        <Page>
          <Story />
        </Page>
      </NextIntlProvider>
    ),
  ],
};

export default preview;
