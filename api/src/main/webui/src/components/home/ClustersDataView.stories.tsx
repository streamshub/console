import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { fn } from 'storybook/test'; // or '@storybook/test', depending on your Storybook version
import type { KafkaCluster, ListResponse } from '@/api/types';
import { ClustersDataView } from './ClustersDataView';
import { KafkaAuthProvider } from '../auth/KafkaAuthProvider';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

const clusters = [
  {
    id: 'kafka-cluster-1',
    attributes: {
      name: 'production-kafka',
      namespace: 'kafka-prod',
      kafkaVersion: '3.9.0',
      status: 'Ready',
    },
    meta: { managed: true, authentication: { method: 'basic' } },
  },
  {
    id: 'kafka-cluster-2',
    attributes: {
      name: 'development-kafka',
      namespace: 'kafka-dev',
      kafkaVersion: '3.8.0',
      status: 'Ready',
    },
    meta: { managed: false, authentication: { method: 'oauth' } },
  },
  {
    id: 'kafka-cluster-3',
    attributes: {
      name: 'local-kafka',
      namespace: 'default',
      kafkaVersion: undefined,
      status: 'Unknown',
    },
    meta: { authentication: { method: 'anonymous' } },
  },
];

// Helper so each story only states what differs
const makeResult = (overrides: Record<string, unknown> = {}) =>
  ({
    data: { data: clusters },
    isLoading: false,
    isPending: false,
    isError: false,
    isSuccess: true,
    error: null,
    status: 'success',
    fetchStatus: 'idle',
    ...overrides,
  } as unknown as UseQueryResult<ListResponse<KafkaCluster>, Error>);

const meta = {
  component: ClustersDataView,
  decorators: [
    (Story) => (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <KafkaAuthProvider>
            <Story />
          </KafkaAuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    ),
  ],
  args: {
    clusterResult: makeResult(),
    onDataViewChange: fn(),
  },
} satisfies Meta<typeof ClustersDataView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: { clusterResult: makeResult({ data: { data: [] } }) },
};

export const Loading: Story = {
  args: {
    clusterResult: makeResult({
      data: undefined,
      isLoading: true,
      isPending: true,
      isSuccess: false,
      status: 'pending',
    }),
  },
};

export const ErrorState: Story = {
  args: {
    clusterResult: makeResult({
      data: undefined,
      isError: true,
      isSuccess: false,
      status: 'error',
      error: new Error('Failed to load clusters'),
    }),
  },
};
