import type { Preview } from '@storybook/react';
import '../src/i18n/config';
import '@patternfly/patternfly/patternfly.css';
import '@patternfly/patternfly/patternfly-charts.css';
import '@patternfly/patternfly/patternfly-addons.css';

const preview: Preview = {
  globalTypes: {
    theme: {
      name: 'Theme',
      defaultValue: 'light',
      toolbar: {
        icon: 'paintbrush',
        items: [
          { value: 'light', title: 'Light' },
          { value: 'dark',  title: 'Dark'  },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
  },

  parameters: {
    layout: 'fullscreen',
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },

  decorators: [
    (Story, context) => {
      const theme = context.globals.theme;
      document.documentElement.classList.remove('pf-v6-theme-dark', 'pf-v6-theme-light');
      document.documentElement.classList.add(
        theme === 'dark' ? 'pf-v6-theme-dark' : 'pf-v6-theme-light',
      );
      return <Story />;
    },
  ],
};

export default preview;
