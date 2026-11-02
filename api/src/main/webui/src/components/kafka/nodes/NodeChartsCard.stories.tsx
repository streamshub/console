import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ListResponse, Node, NodeListMeta } from '@/api/types';
import { NodeChartsCard } from './NodeChartsCard';

const createNodeResult = (
  nodes: Node[],
  isLoading = false
): UseQueryResult<ListResponse<Node, NodeListMeta>, Error> =>
  ({
    data: { data: nodes },
    error: null,
    isLoading,
    isPending: isLoading,
    isError: false,
    isSuccess: !isLoading,
    isFetching: false,
    status: isLoading ? 'pending' : 'success',
    fetchStatus: isLoading ? 'fetching' : 'idle',
  } as unknown as UseQueryResult<ListResponse<Node, NodeListMeta>, Error>);

const meta: Meta<typeof NodeChartsCard> = {
  component: NodeChartsCard,
  args: {
    nodeResult: createNodeResult([]),
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Loading: Story = {
  args: {
    nodeResult: createNodeResult([], true),
  },
};

export const Empty: Story = {
  args: {
    nodeResult: createNodeResult([]),
  },
};
