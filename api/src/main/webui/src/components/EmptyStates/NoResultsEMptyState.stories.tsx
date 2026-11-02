import type { Meta, StoryObj } from '@storybook/react';
import { NoResultsEmptyState } from './NoResultsEmptyState';

const meta: Meta<typeof NoResultsEmptyState> = {
  component: NoResultsEmptyState,
  argTypes: {
    title: {
      control: 'text',
      description: 'Optional title displayed above the description.',
    },
    message: {
      control: 'text',
      description: 'Optional description displayed below the title.',
    },
  },
};

export default meta;

type Story = StoryObj<typeof NoResultsEmptyState>;

export const Default: Story = {};

export const WithCustomContent: Story = {
  args: {
    title: 'No users found',
    message: 'Try adjusting your search criteria or filters.',
  },
};

export const WithCustomTitle: Story = {
  args: {
    title: 'No matching records',
  },
};

export const WithCustomMessage: Story = {
  args: {
    message: 'No results match your current filters.',
  },
};

export const WithLongMessage: Story = {
  args: {
    title: 'No results found',
    message:
      'We could not find any results matching your search. Try using different keywords, removing some filters, or broadening your search criteria.',
  },
};
