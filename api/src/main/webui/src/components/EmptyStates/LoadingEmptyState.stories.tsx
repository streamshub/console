import type { Meta, StoryObj } from '@storybook/react';
import { LoadingEmptyState } from './LoadingEmptyState';

const meta = {
  component: LoadingEmptyState,
  args: {},
} satisfies Meta<typeof LoadingEmptyState>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithMessage: Story = {
  args: {
    message: 'Loading Kafka resources. Please wait...',
  },
};
