import type { Meta, StoryObj } from '@storybook/react';
import { TimestampPicker } from './TimeStampPicker';

const meta: Meta<typeof TimestampPicker> = {
  component: TimestampPicker,
};

export default meta;
type Story = StoryObj<typeof TimestampPicker>;

export const Default: Story = {
  args: {
    value: undefined,
    onCommit: (value) => console.log('Timestamp committed:', value),
  },
};

export const WithTimestamp: Story = {
  args: {
    value: '2026-10-15T10:30:45.123+05:30',
    onCommit: (value) => console.log('Timestamp committed:', value),
  },
};

export const WithUTCTimestamp: Story = {
  args: {
    value: '2026-10-15T05:00:00.000Z',
    onCommit: (value) => console.log('Timestamp committed:', value),
  },
};
