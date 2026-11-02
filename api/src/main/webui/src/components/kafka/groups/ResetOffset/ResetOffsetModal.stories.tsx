import type { Meta, StoryObj } from '@storybook/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { Group } from '@/api/types';
import { ResetOffsetModal } from './ResetOffsetModal';

const mockGroup = {
  id: 'group-1',
  attributes: {
    groupId: 'orders-consumer-group',
    offsets: [
      {
        topicId: 'topic-1',
        topicName: 'orders',
        partition: 0,
        offset: 120,
      },
      {
        topicId: 'topic-1',
        topicName: 'orders',
        partition: 1,
        offset: 250,
      },
      {
        topicId: 'topic-2',
        topicName: 'payments',
        partition: 0,
        offset: 80,
      },
    ],
  },
} as unknown as Group;

const meta: Meta<typeof ResetOffsetModal> = {
  component: ResetOffsetModal,
  decorators: [
    (Story) => {
      const queryClient = new QueryClient({
        defaultOptions: {
          queries: {
            retry: false,
          },
          mutations: {
            retry: false,
          },
        },
      });

      return (
        <QueryClientProvider client={queryClient}>
          <Story />
        </QueryClientProvider>
      );
    },
  ],
  args: {
    isOpen: true,
    group: mockGroup,
    kafkaId: 'kafka-1',
    onClose: () => {},
    onSuccess: () => {},
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const NoOffsets: Story = {
  args: {
    group: {
      ...mockGroup,
      attributes: {
        ...mockGroup.attributes,
        offsets: [],
      },
    } as Group,
  },
};
