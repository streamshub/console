import type { Meta, StoryObj } from '@storybook/react';
import { MemoryRouter } from 'react-router';
import { LagTable } from './LagTable';
import type { OffsetAndMetadata } from '@/api/types';

const mockOffsets: OffsetAndMetadata[] = [
  {
    topicId: 'topic-1',
    topicName: 'orders',
    partition: 0,
    lag: 150,
    offset: 850,
    logEndOffset: 1000,
  },
  {
    topicId: 'topic-1',
    topicName: 'orders',
    partition: 1,
    lag: 0,
    offset: 1200,
    logEndOffset: 1200,
  },
  {
    topicId: 'topic-2',
    topicName: 'payments',
    partition: 0,
    lag: 25,
    offset: 475,
    logEndOffset: 500,
  },
];

const meta = {
  component: LagTable,
  decorators: [
    (Story) => (
      <MemoryRouter>
        {/* Links never navigate in Storybook */}
        <div
          onClickCapture={(e) => {
            if ((e.target as HTMLElement).closest('a')) e.preventDefault();
          }}
        >
          <Story />
        </div>
      </MemoryRouter>
    ),
  ],
  args: { offsets: mockOffsets },
} satisfies Meta<typeof LagTable>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const NoLag: Story = {
  args: {
    offsets: [
      {
        topicId: 'topic-1',
        topicName: 'orders',
        partition: 0,
        lag: 0,
        offset: 1000,
        logEndOffset: 1000,
      },
      {
        topicId: 'topic-1',
        topicName: 'orders',
        partition: 1,
        lag: 0,
        offset: 500,
        logEndOffset: 500,
      },
    ],
  },
};

export const Empty: Story = { args: { offsets: [] } };

export const TopicWithoutId: Story = {
  args: {
    offsets: [
      {
        topicId: '',
        topicName: 'unknown-topic',
        partition: 0,
        lag: 10,
        offset: 90,
        logEndOffset: 100,
      },
    ],
  },
};
