import type { Meta, StoryObj } from '@storybook/react-vite'; // or '@storybook/react'
import { MemoryRouter, Route, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TopicConfigurationTab } from './TopicConfigurationTab';

const KAFKA_ID = 'k1-id';
const TOPIC_ID = 'topic-1';

const configs = {
  'cleanup.policy': {
    type: 'LIST',
    value: 'delete',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'retention.ms': {
    type: 'LONG',
    value: '604800000',
    source: 'DYNAMIC_TOPIC_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'max.message.bytes': {
    type: 'INT',
    value: '1048588',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'segment.bytes': {
    type: 'INT',
    value: '1073741824',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: true,
  },
};

const originalFetch = window.fetch;

function withTopic(topicConfigs: object, privileges: string[] = ['UPDATE']) {
  return (Story: React.ComponentType) => {
    const body = {
      data: {
        id: TOPIC_ID,
        type: 'topics',
        attributes: { name: 'orders', configs: topicConfigs },
        meta: { privileges }, // adjust to whatever hasPrivilege() reads
      },
    };

    // Answer any /api/ request with the mock topic, pass everything else through
    window.fetch = async (input, init) => {
      const url =
        typeof input === 'string'
          ? input
          : input instanceof URL
          ? input.href
          : input.url;

      if (url.includes('/api/')) {
        return new Response(JSON.stringify(body), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }
      return originalFetch(input, init);
    };

    // New client per render so cached data doesn't leak between stories
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[
            `/kafka/${KAFKA_ID}/topics/${TOPIC_ID}/configuration`,
          ]}
        >
          <Routes>
            <Route
              path="/kafka/:kafkaId/topics/:topicId/configuration"
              element={<Story />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };
}
const meta = {
  component: TopicConfigurationTab,
} satisfies Meta<typeof TopicConfigurationTab>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  decorators: [withTopic(configs)],
};

export const ReadOnlyUser: Story = {
  decorators: [withTopic(configs, [])],
};

export const NoConfiguration: Story = {
  decorators: [withTopic({})],
};
