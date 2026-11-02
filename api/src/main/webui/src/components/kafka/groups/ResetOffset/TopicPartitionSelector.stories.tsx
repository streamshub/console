import type { Meta, StoryObj } from '@storybook/react';
import { useArgs } from 'storybook/preview-api';
import { TopicPartitionSelector } from './TopicPartitionSelector';

const topics = [
  { id: 'topic-1', name: 'orders' },
  { id: 'topic-2', name: 'payments' },
  { id: 'topic-3', name: 'inventory' },
  { id: 'topic-4', name: 'customer-events' },
];

const meta: Meta<typeof TopicPartitionSelector> = {
  component: TopicPartitionSelector,
  args: {
    topics,
    partitions: [0, 1, 2, 3],
    selectedTopicId: undefined,
    selectedPartition: undefined,
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: function Render(args) {
    const [{ selectedTopicId, selectedPartition }, updateArgs] = useArgs();

    const selectedTopicPartitions =
      selectedTopicId === 'topic-1'
        ? [0, 1]
        : selectedTopicId === 'topic-2'
        ? [0, 1, 2]
        : selectedTopicId === 'topic-3'
        ? [0, 1, 2, 3]
        : selectedTopicId === 'topic-4'
        ? [0, 1]
        : [];

    return (
      <TopicPartitionSelector
        {...args}
        selectedTopicId={selectedTopicId as string | undefined}
        selectedPartition={selectedPartition as number | undefined}
        partitions={selectedTopicId ? selectedTopicPartitions : args.partitions}
        onTopicChange={(topicId) =>
          updateArgs({
            selectedTopicId: topicId,
            selectedPartition: undefined,
          })
        }
        onPartitionChange={(partition) =>
          updateArgs({ selectedPartition: partition })
        }
      />
    );
  },
};

export const NoTopics: Story = {
  args: {
    topics: [],
    partitions: [],
  },
};

export const TopicSelected: Story = {
  args: {
    selectedTopicId: 'topic-1',
    partitions: [0, 1],
  },
};

export const PartitionSelected: Story = {
  args: {
    selectedTopicId: 'topic-1',
    selectedPartition: 1,
    partitions: [0, 1],
  },
};

export const ManyTopics: Story = {
  args: {
    topics: Array.from({ length: 20 }, (_, index) => ({
      id: `topic-${index + 1}`,
      name: `topic-${index + 1}`,
    })),
    partitions: [0, 1, 2, 3, 4, 5],
  },
};
