import type { Meta, StoryObj } from '@storybook/react';
import { DryRunResults } from './DryRunResults';

const mockResults = [
  {
    topicName: 'orders',
    partition: 0,
    offset: 120,
  },
  {
    topicName: 'orders',
    partition: 1,
    offset: 250,
  },
  {
    topicName: 'payments',
    partition: 0,
    offset: 80,
  },
];

const mockCurrentOffsets = [
  {
    topicName: 'orders',
    partition: 0,
    offset: 100,
  },
  {
    topicName: 'orders',
    partition: 1,
    offset: 200,
  },
  {
    topicName: 'payments',
    partition: 0,
    offset: 60,
  },
];

const meta: Meta<typeof DryRunResults> = {
  component: DryRunResults,
  args: {
    isOpen: true,
    results: mockResults,
    currentOffsets: mockCurrentOffsets,
    command:
      'kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group orders-consumer-group --reset-offsets --to-earliest --execute',
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const MultipleTopics: Story = {
  args: {
    results: [
      ...mockResults,
      {
        topicName: 'inventory',
        partition: 0,
        offset: 45,
      },
      {
        topicName: 'inventory',
        partition: 1,
        offset: null,
      },
    ],
  },
};

export const NoResults: Story = {
  args: {
    results: [],
    currentOffsets: [],
  },
};

export const WithoutCurrentOffsets: Story = {
  args: {
    currentOffsets: null,
  },
};
