import type { Meta, StoryObj } from '@storybook/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ConnectCluster, ListResponse } from '@/api/types';
import { ConnectClustersDataView } from './ConnectClustersDataView';

const mockConnectClusters = [
  {
    id: 'connect-cluster-1',
    attributes: {
      name: 'connect-cluster-east',
      version: '3.9.0',
      replicas: 3,
    },
  },
  {
    id: 'connect-cluster-2',
    attributes: {
      name: 'connect-cluster-west',
      version: '3.8.1',
      replicas: 2,
    },
  },
  {
    id: 'connect-cluster-3',
    attributes: {
      name: 'connect-cluster-dev',
      version: '',
      replicas: undefined,
    },
  },
] as unknown as ConnectCluster[];

const createQueryResult = (
  clusters: ConnectCluster[]
): UseQueryResult<ListResponse<ConnectCluster>, Error> =>
  ({
    data: { data: clusters },
    error: null,
    isLoading: false,
    isPending: false,
    isError: false,
    isSuccess: true,
    isFetching: false,
    isRefetching: false,
    status: 'success',
    fetchStatus: 'idle',
    refetch: async () => ({} as never),
  } as unknown as UseQueryResult<ListResponse<ConnectCluster>, Error>);

const meta: Meta<typeof ConnectClustersDataView> = {
  component: ConnectClustersDataView,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/kafka/kafka-1/connect/clusters']}>
        <Routes>
          <Route path="/kafka/:kafkaId/connect/clusters" element={<Story />} />
        </Routes>
      </MemoryRouter>
    ),
  ],
  args: {
    connectClustersResult: createQueryResult(mockConnectClusters),
    onDataViewChange: () => {},
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: {
    connectClustersResult: createQueryResult([]),
  },
};
