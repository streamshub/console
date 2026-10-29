import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { OffsetValueSelector } from './OffsetValueSelector';
import { OffsetValue, TopicSelection, PartitionSelection } from './types';

type StoryProps = React.ComponentProps<typeof OffsetValueSelector>;

const meta: Meta<typeof OffsetValueSelector> = {
  component: OffsetValueSelector,
  args: {},
};

export default meta;
type Story = StoryObj<typeof OffsetValueSelector>;

const ControlledOffsetValueSelector = (args: StoryProps) => {
  const [offsetValue, setOffsetValue] = useState<OffsetValue | undefined>(
    args.offsetValue
  );
  const [customOffset, setCustomOffset] = useState<number | undefined>(
    args.customOffset
  );
  const [dateTime, setDateTime] = useState<string | undefined>(args.dateTime);
  const [dateTimeDisplayMode, setDateTimeDisplayMode] = useState<
    'utc' | 'local'
  >(args.dateTimeDisplayMode || 'utc');

  return (
    <OffsetValueSelector
      {...args}
      offsetValue={offsetValue}
      customOffset={customOffset}
      dateTime={dateTime}
      dateTimeDisplayMode={dateTimeDisplayMode}
      onOffsetValueChange={(val) => {
        setOffsetValue(val);
        args.onOffsetValueChange?.(val);
      }}
      onCustomOffsetChange={(val) => {
        setCustomOffset(val);
        args.onCustomOffsetChange?.(val);
      }}
      onDateTimeChange={(val) => {
        setDateTime(val);
        args.onDateTimeChange?.(val);
      }}
      onDateTimeDisplayModeChange={(mode) => {
        setDateTimeDisplayMode(mode);
        args.onDateTimeDisplayModeChange?.(mode);
      }}
    />
  );
};

export const Interactive: Story = {
  render: (args) => <ControlledOffsetValueSelector {...args} />,
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'selectedPartition' as PartitionSelection,
    offsetValue: 'earliest',
  },
};

export const AllTopicsAndPartitions: Story = {
  render: (args) => <ControlledOffsetValueSelector {...args} />,
  args: {
    topicSelection: 'allTopics' as TopicSelection,
    partitionSelection: 'allPartitions' as PartitionSelection,
    offsetValue: 'latest',
  },
};

export const CustomOffset: Story = {
  render: (args) => <ControlledOffsetValueSelector {...args} />,
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'selectedPartition' as PartitionSelection,
    offsetValue: 'custom',
    customOffset: 104250,
  },
};

export const DateTimeIsoUtc: Story = {
  render: (args) => <ControlledOffsetValueSelector {...args} />,
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'allPartitions' as PartitionSelection,
    offsetValue: 'dateTimeIso',
    dateTime: '2026-10-29T12:00:00.000Z',
    dateTimeDisplayMode: 'utc',
  },
};

export const DateTimeIsoLocal: Story = {
  render: (args) => <ControlledOffsetValueSelector {...args} />,
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'allPartitions' as PartitionSelection,
    offsetValue: 'dateTimeIso',
    dateTime: '2026-10-29T17:30:00.000+05:30',
    dateTimeDisplayMode: 'local',
  },
};

export const DateTimeEpoch: Story = {
  render: (args) => <ControlledOffsetValueSelector {...args} />,
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'allPartitions' as PartitionSelection,
    offsetValue: 'dateTimeEpoch',
    dateTime: '1793275200000',
  },
};

export const CustomOffsetWithError: Story = {
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'selectedPartition' as PartitionSelection,
    offsetValue: 'custom',
    customOffset: -5,
    errors: {
      customOffset: 'Offset must be a positive integer or 0',
    },
  },
};

export const DateTimeWithError: Story = {
  args: {
    topicSelection: 'selectedTopic' as TopicSelection,
    partitionSelection: 'allPartitions' as PartitionSelection,
    offsetValue: 'dateTimeIso',
    dateTime: 'invalid-date-string',
    errors: {
      dateTime: 'Please provide a valid ISO-8601 date string',
    },
  },
};
