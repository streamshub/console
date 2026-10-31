import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { fn } from 'storybook/test'; // or '@storybook/test', matching your other stories
import type { ListResponse, Node, NodeListMeta } from '@/api/types';
import { NodesDataView } from './NodesDataView';

const nodes = [
  {
    id: '0',
    meta: { privileges: ['GET'] },
    attributes: {
      roles: ['broker', 'controller'],
      kafkaVersion: '3.9.0',
      rack: 'zone-a',
      nodePool: 'dual-role',
      metadataState: { status: 'leader' },
      broker: { status: 'Running', leaderCount: 12, replicaCount: 30 },
      controller: { status: 'QuorumLeader' },
    },
  },
  {
    id: '1',
    meta: { privileges: ['GET'] },
    attributes: {
      roles: ['broker'],
      kafkaVersion: '3.9.0',
      rack: 'zone-b',
      nodePool: 'brokers',
      broker: { status: 'Running', leaderCount: 10, replicaCount: 28 },
    },
  },
  {
    id: '2',
    meta: { privileges: [] }, // no GET privilege, so the id is plain text, not a link
    attributes: {
      roles: ['controller'],
      kafkaVersion: '3.9.0',
      rack: undefined,
      nodePool: undefined,
      controller: { status: 'QuorumFollower' },
    },
  },
] as unknown as Node[];

const listMeta = {
  summary: {
    nodePools: {
      'dual-role': { count: 1, roles: ['broker', 'controller'] },
      brokers: { count: 1, roles: ['broker'] },
    },
  },
} as unknown as NodeListMeta;

// Helper so each story only states what differs
const makeResult = (overrides: Record<string, unknown> = {}) =>
  ({
    data: { data: nodes, meta: listMeta },
    isLoading: false,
    isPending: false,
    isError: false,
    isSuccess: true,
    error: null,
    status: 'success',
    fetchStatus: 'idle',
    ...overrides,
  } as unknown as UseQueryResult<ListResponse<Node, NodeListMeta>, Error>);

const meta = {
  component: NodesDataView,
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
      </MemoryRouter>
    ),
  ],
  args: {
    kafkaId: 'kafka-1',
    nodeResult: makeResult(),
    onDataViewChange: fn(),
  },
} satisfies Meta<typeof NodesDataView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: { nodeResult: makeResult({ data: { data: [], meta: listMeta } }) },
};

export const Loading: Story = {
  args: {
    nodeResult: makeResult({
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
    nodeResult: makeResult({
      data: undefined,
      isError: true,
      isSuccess: false,
      status: 'error',
      error: new Error('Failed to load nodes'),
    }),
  },
};
