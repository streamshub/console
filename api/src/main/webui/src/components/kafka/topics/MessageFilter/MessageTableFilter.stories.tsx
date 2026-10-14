import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { MessageTableFilter } from './MessageTableFilter';
import type { FilterState } from './messageFIlter';

const meta: Meta<typeof MessageTableFilter> = {
  component: MessageTableFilter,
};

export default meta;
type Story = StoryObj<typeof MessageTableFilter>;

const defaultValue: FilterState = {
  partition: undefined,
  from: 'latest',
  offset: undefined,
  limit: 50,
  query: '',
  where: undefined,
};

interface FilterStoryProps {
  partitions?: number[];
  initialValue?: FilterState;
}

function FilterStory({
  partitions = [0, 1, 2, 3, 4],
  initialValue = defaultValue,
}: FilterStoryProps) {
  const [value, setValue] = useState<FilterState>(initialValue);

  return (
    <MessageTableFilter
      partitions={partitions}
      value={value}
      onChange={(patch) => setValue((current) => ({ ...current, ...patch }))}
    />
  );
}

export const Latest: Story = {
  render: () => <FilterStory />,
};

export const Last5Minutes: Story = {
  render: () => (
    <FilterStory initialValue={{ ...defaultValue, from: 'last5' }} />
  ),
};

export const Last10Minutes: Story = {
  render: () => (
    <FilterStory initialValue={{ ...defaultValue, from: 'last10' }} />
  ),
};

export const Last15Minutes: Story = {
  render: () => (
    <FilterStory initialValue={{ ...defaultValue, from: 'last15' }} />
  ),
};

export const FetchFromOffset: Story = {
  render: () => (
    <FilterStory
      initialValue={{ ...defaultValue, from: 'offset', offset: 123 }}
    />
  ),
};

export const FetchFromOffsetWithoutValue: Story = {
  render: () => (
    <FilterStory
      initialValue={{ ...defaultValue, from: 'offset', offset: undefined }}
    />
  ),
};

export const FetchFromTimestamp: Story = {
  render: () => (
    <FilterStory
      initialValue={{
        ...defaultValue,
        from: 'timestamp',
        query: 'payment',
      }}
    />
  ),
};

export const SelectedPartition: Story = {
  render: () => (
    <FilterStory
      initialValue={{
        ...defaultValue,
        partition: 2,
        from: 'last10',
      }}
    />
  ),
};

export const SinglePartition: Story = {
  render: () => (
    <FilterStory
      partitions={[0]}
      initialValue={{ ...defaultValue, partition: 0 }}
    />
  ),
};

export const NoPartitions: Story = {
  render: () => <FilterStory partitions={[]} />,
};

export const LargeLimit: Story = {
  render: () => (
    <FilterStory
      initialValue={{
        ...defaultValue,
        limit: 500,
        from: 'last10',
      }}
    />
  ),
};
