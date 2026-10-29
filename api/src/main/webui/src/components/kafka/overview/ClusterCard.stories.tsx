import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ClusterCard } from './ClusterCard';
import type { KafkaCluster } from '@/api/types';

const cluster = {
  id: 'cluster-1',
  type: 'kafkas',
  attributes: {
    name: 'my-kafka-cluster',
    status: 'Ready',
    kafkaVersion: '3.8.0',
    conditions: [],
  },
  meta: {
    managed: true,
    reconciliationPaused: false,
    privileges: ['UPDATE'],
  },
} as unknown as KafkaCluster;

const withConditions = (
  conditions: { type: string; message: string; lastTransitionTime: string }[],
  meta = {}
) =>
  ({
    ...cluster,
    attributes: { ...cluster.attributes, conditions },
    meta: { ...cluster.meta, ...meta },
  } as unknown as KafkaCluster);

const meta = {
  component: ClusterCard,
  decorators: [
    (Story) => (
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <Story />
        </MemoryRouter>
      </QueryClientProvider>
    ),
  ],
  args: {
    cluster,
    groupsCount: 12,
    brokersOnline: 3,
    brokersTotal: 3,
    isLoading: false,
  },
} satisfies Meta<typeof ClusterCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Loading: Story = {
  args: { isLoading: true },
};

export const BrokersDown: Story = {
  args: {
    brokersOnline: 2,
    cluster: withConditions([]),
    groupsCount: 0,
  },
};

export const WithErrorsAndWarnings: Story = {
  args: {
    cluster: withConditions([
      {
        type: 'Error',
        message: 'Broker 1 is not reachable.',
        lastTransitionTime: '2025-01-15T10:00:00Z',
      },
      {
        type: 'Warning',
        message: 'Kafka version is outdated.',
        lastTransitionTime: '2025-01-14T08:30:00Z',
      },
    ]),
  },
};

export const ReconciliationPaused: Story = {
  args: {
    cluster: withConditions(
      [
        {
          type: 'ReconciliationPaused',
          message: 'Reconciliation is paused.',
          lastTransitionTime: '2025-01-15T10:00:00Z',
        },
      ],
      { reconciliationPaused: true }
    ),
  },
};

export const NotManaged: Story = {
  args: {
    cluster: withConditions([], { managed: false }),
  },
};
