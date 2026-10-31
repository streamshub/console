import type { Meta, StoryObj } from '@storybook/react';
import { ErrorEmptyState } from './ErrorEmptyState';

const meta: Meta<typeof ErrorEmptyState> = {
  component: ErrorEmptyState,
};

export default meta;

type Story = StoryObj<typeof ErrorEmptyState>;

export const WithErrorMessage: Story = {
  args: {
    error: new Error('Failed to load data.'),
  },
};

export const CustomMessage: Story = {
  args: {
    title: 'Unable to load topics',
    message: 'Please try again later.',
  },
};
