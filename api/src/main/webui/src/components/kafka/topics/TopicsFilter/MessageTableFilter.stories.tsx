import type { Meta, StoryObj } from '@storybook/react';
import { MessageTableFilter } from './MessageTableFilter';

const meta: Meta<typeof MessageTableFilter> = {
  component: MessageTableFilter,
};

export default meta;
type Story = StoryObj<typeof MessageTableFilter>;

const defaultArgs = {
  partitions: [0, 1, 2, 3, 4],
  selectedPartition: undefined,
  selectedFrom: 'latest' as const,
  limit: 50,
  query: '',
  where: undefined,
};

export const Latest: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'latest',
  },
};

export const Last5Minutes: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'last5',
  },
};

export const Last10Minutes: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'last10',
  },
};

export const Last15Minutes: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'last15',
  },
};

export const FetchFromOffset: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'offset',
    offset: 123,
  },
};

export const FetchFromOffsetWithoutValue: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'offset',
    offset: undefined,
  },
};

export const FetchFromTimestamp: Story = {
  args: {
    ...defaultArgs,
    selectedFrom: 'timestamp',
    query: 'payment',
  },
};

export const SelectedPartition: Story = {
  args: {
    ...defaultArgs,
    selectedPartition: 2,
    selectedFrom: 'last10',
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

export const LargeLimit: Story = {
  args: {
    ...defaultArgs,
    limit: 500,
    selectedFrom: 'last10',
  },
};
