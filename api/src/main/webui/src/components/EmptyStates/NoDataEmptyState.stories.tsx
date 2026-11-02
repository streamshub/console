import type { Meta, StoryObj } from '@storybook/react';
import { SearchIcon } from '@patternfly/react-icons';
import { NoDataEmptyState } from './NoDataEmptyState';

const meta = {
  component: NoDataEmptyState,
} satisfies Meta<typeof NoDataEmptyState>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithEntityName: Story = {
  args: { entityName: 'topics' },
};

export const CustomTitleAndMessage: Story = {
  args: {
    title: 'No connectors yet',
    message: 'Create a connector to start moving data in or out of Kafka.',
  },
};

export const CustomIcon: Story = {
  args: {
    icon: SearchIcon,
    title: 'No results found',
    message: 'Try changing your search.',
  },
};

export const WithLearningLink: Story = {
  args: {
    entityName: 'consumer groups',
    learningLink: {
      href: 'https://example.com/docs',
      label: 'Learn more about consumer groups',
    },
  },
};
