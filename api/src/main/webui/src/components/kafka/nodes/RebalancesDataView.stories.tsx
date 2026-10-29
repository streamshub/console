import type { Meta, StoryObj } from '@storybook/react';
import { MemoryRouter } from 'react-router';
import type { UseQueryResult } from '@tanstack/react-query';
import type { Rebalance, ListResponse } from '@/api/types';
import type { ResourceListParams } from '@/api/hooks/useResourceList';
import { RebalancesDataView } from './RebalancesDataView';

const mockRebalances = [
  {
    id: 'rebalance-1',
    attributes: {
      name: 'rebalance-east',
      status: 'ProposalReady',
      mode: 'full',
      creationTimestamp: '2026-10-28T10:00:00Z',
      optimizationResult: {
        dataToMoveMB: 256,
        numReplicaMovements: 12,
        numLeaderMovements: 4,
      },
    },
    meta: {
      autoApproval: false,
      allowedActions: ['approve', 'refresh', 'stop'],
    },
  },
] as unknown as Rebalance[];

const createQueryResult = (
  rebalances: Rebalance[]
): UseQueryResult<ListResponse<Rebalance>, Error> =>
  ({
    data: { data: rebalances },
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
  } as unknown as UseQueryResult<ListResponse<Rebalance>, Error>);

const meta = {
  component: RebalancesDataView,
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
      </MemoryRouter>
    ),
  ],
  args: {
    kafkaId: 'kafka-1',
    rebalanceResult: createQueryResult(mockRebalances),
    onDataViewChange: (_params: ResourceListParams) => {},
    onApprove: (_rebalance: Rebalance) => {},
    onStop: (_rebalance: Rebalance) => {},
    onRefresh: (_rebalance: Rebalance) => {},
  },
} satisfies Meta<typeof RebalancesDataView>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: {
    rebalanceResult: createQueryResult([]),
  },
};
