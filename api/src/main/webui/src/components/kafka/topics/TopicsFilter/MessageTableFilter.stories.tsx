import type { Meta, StoryObj } from '@storybook/react';
import { NewFilter } from './MessageTableFilter';

const meta: Meta<typeof NewFilter> = {
  component: NewFilter,
};

export default meta;
type Story = StoryObj<typeof NewFilter>;

const defaultArgs = {
  partitions: [0, 1, 2, 3, 4],
  selectedFrom: 'latest' as const,
  limit: 50,
  onPartitionChange: (partition?: number) => {
    console.log('Selected partition:', partition);
  },
  onFromChange: (
    from: 'latest' | 'last5' | 'last10' | 'last15' | 'offset' | 'timestamp'
  ) => {
    console.log('Selected fetch from:', from);
  },
  onLimitChange: (limit: number) => {
    console.log('Selected limit:', limit);
  },
};

export const Default: Story = {
  args: {
    ...defaultArgs,
  },
};

export const SelectedPartition: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'offset',
    selectedPartition: 2,
  },
};

export const SinglePartition: Story = {
  args: {
    ...defaultArgs,
    partitions: [0],
    selectedPartition: 0,
  },
};

export const NoPartitions: Story = {
  args: {
    ...defaultArgs,
    partitions: [],
  },
};

export const Last10Minutes: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'last10',
  },
};

export const LargeLimit: Story = {
  args: {
    ...defaultArgs,
    limit: 500,
  },
};
