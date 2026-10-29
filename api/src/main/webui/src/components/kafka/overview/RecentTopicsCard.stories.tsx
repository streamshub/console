import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RecentTopicsCard } from './RecentTopicsCard';
import type { ViewedTopic } from '@/api/hooks/useViewedTopics';

const viewedTopics = [
  { topicId: 'topic-1', topicName: 'orders' },
  { topicId: 'topic-2', topicName: 'payments' },
  { topicId: 'topic-3', topicName: 'shipments' },
  { topicId: 'topic-4', topicName: 'notifications' },
  { topicId: 'topic-5', topicName: 'audit-log' },
] as ViewedTopic[];

const meta = {
  component: RecentTopicsCard,
  decorators: [
    (Story) => (
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <MemoryRouter>
          <div>
            <Story />
          </div>
        </MemoryRouter>
      </QueryClientProvider>
    ),
  ],
  args: {
    viewedTopics,
    isLoading: false,
  },
} satisfies Meta<typeof RecentTopicsCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Loading: Story = {
  args: { isLoading: true },
};

export const Empty: Story = {
  args: { viewedTopics: [] },
};
