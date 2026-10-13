import type { Meta, StoryObj } from '@storybook/react';
import { NewFilter } from './MessageTableFilter';

const meta: Meta<typeof NewFilter> = {
  component: NewFilter,
  title: 'Kafka/Messages/Filters/NewFilter',
};

export default meta;
type Story = StoryObj<typeof NewFilter>;

export const Default: Story = {
  args: {
    partitions: [0, 1, 2, 3, 4],
    onPartitionChange: (partition) => {
      console.log('Selected partition:', partition);
    },
  },
};

export const SelectedPartition: Story = {
  args: {
    partitions: [0, 1, 2, 3, 4],
    selectedPartition: 2,
    onPartitionChange: (partition) => {
      console.log('Selected partition:', partition);
    },
  },
};

export const SinglePartition: Story = {
  args: {
    partitions: [0],
    selectedPartition: 0,
    onPartitionChange: (partition) => {
      console.log('Selected partition:', partition);
    },
  },
};

export const NoPartitions: Story = {
  args: {
    partitions: [],
    onPartitionChange: (partition) => {
      console.log('Selected partition:', partition);
    },
  },
};
