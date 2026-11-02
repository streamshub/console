import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import type { ListResponse, Topic } from '@/api/types';
import { TopicsDataView } from './TopicsDataView';

const topics = [
  {
    id: 'topic-1',
    meta: { managed: true },
    attributes: {
      name: 'orders',
      status: 'FullyReplicated',
      numPartitions: 6,
      totalLeaderLogBytes: 1073741824,
    },
    relationships: { groups: { meta: { count: 3 } } },
  },
  {
    id: 'topic-2',
    meta: { managed: false },
    attributes: {
      name: 'payments',
      status: 'UnderReplicated',
      numPartitions: 3,
      totalLeaderLogBytes: 52428800,
    },
    relationships: { groups: { meta: { count: 0 } } },
  },
  {
    id: 'topic-3',
    attributes: {
      name: 'audit-log',
      status: undefined,
      numPartitions: null,
      totalLeaderLogBytes: undefined,
    },
    relationships: {},
  },
] as unknown as Topic[];

// Helper so each story only states what differs
const makeResult = (overrides: Record<string, unknown> = {}) =>
  ({
    data: { data: topics },
    isLoading: false,
    isPending: false,
    isError: false,
    isSuccess: true,
    error: null,
    status: 'success',
    fetchStatus: 'idle',
    ...overrides,
  } as unknown as UseQueryResult<ListResponse<Topic>, Error>);

const meta = {
  component: TopicsDataView,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/kafka/kafka-1/topics']}>
        <Story />
      </MemoryRouter>
    ),
  ],
  args: {
    topicResult: makeResult(),
    onDataViewChange: () => {},
  },
} satisfies Meta<typeof TopicsDataView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: { topicResult: makeResult({ data: { data: [] } }) },
};

export const Loading: Story = {
  args: {
    topicResult: makeResult({
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
    topicResult: makeResult({
      data: undefined,
      isError: true,
      isSuccess: false,
      status: 'error',
      error: new Error('Failed to load topics'),
    }),
  },
};
