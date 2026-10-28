import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router';
import { fn } from 'storybook/test'; // or '@storybook/test', matching the other stories
import type { EnrichedConnector, ListResponse } from '@/api/types';
import { ConnectorsDataView } from './ConnectorsDataView';

const connectors = [
  {
    id: 'connector-1',
    attributes: { name: 'orders-source', type: 'source', state: 'RUNNING' },
    connectClusterId: 'connect-1',
    connectClusterName: 'prod-connect',
    replicas: 3,
    meta: { managed: true },
  },
  {
    id: 'connector-2',
    attributes: { name: 'orders-sink', type: 'sink', state: 'FAILED' },
    connectClusterId: 'connect-1',
    connectClusterName: 'prod-connect',
    replicas: 1,
    meta: { managed: false },
  },
  {
    id: 'connector-3',
    attributes: { name: 'mirror-source', type: 'source:mm', state: 'PAUSED' },
    connectClusterId: undefined,
    connectClusterName: undefined,
    replicas: undefined,
    meta: {},
  },
];

const makeResult = (overrides: Record<string, unknown> = {}) =>
  ({
    data: { data: connectors },
    isLoading: false,
    isPending: false,
    isError: false,
    isSuccess: true,
    error: null,
    status: 'success',
    fetchStatus: 'idle',
    ...overrides,
  } as unknown as UseQueryResult<ListResponse<EnrichedConnector>, Error>);

const meta = {
  title: 'Kafka/Connect/ConnectorsDataView',
  component: ConnectorsDataView,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/kafka/my-kafka/connect/connectors']}>
        <Routes>
          <Route
            path="/kafka/:kafkaId/connect/connectors"
            element={<Story />}
          />
        </Routes>
      </MemoryRouter>
    ),
  ],
  args: {
    connectorsResult: makeResult(),
    onDataViewChange: fn(),
  },
} satisfies Meta<typeof ConnectorsDataView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: { connectorsResult: makeResult({ data: { data: [] } }) },
};

export const Loading: Story = {
  args: {
    connectorsResult: makeResult({
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
    connectorsResult: makeResult({
      data: undefined,
      isError: true,
      isSuccess: false,
      status: 'error',
      error: new Error('Failed to load connectors'),
    }),
  },
};
