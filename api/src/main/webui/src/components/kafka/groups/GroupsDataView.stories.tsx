import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { fn } from 'storybook/test'; // or '@storybook/test', matching your other stories
import type { Group, ListResponse } from '@/api/types';
import { GroupsDataView } from './GroupsDataView';

const groups = [
  {
    id: 'group-1',
    meta: { describeAvailable: true, privileges: ['UPDATE'] },
    attributes: {
      groupId: 'orders-consumer',
      type: 'Consumer',
      protocol: 'consumer',
      state: 'EMPTY',
      members: [],
      offsets: [
        {
          topicId: 'topic-1',
          topicName: 'orders',
          partition: 0,
          lag: 150,
          offset: 850,
          logEndOffset: 1000,
        },
        {
          topicId: 'topic-1',
          topicName: 'orders',
          partition: 1,
          lag: 25,
          offset: 475,
          logEndOffset: 500,
        },
      ],
    },
  },
  {
    id: 'group-2',
    meta: { describeAvailable: true, privileges: ['UPDATE'] },
    attributes: {
      groupId: 'payments-consumer',
      type: 'Classic',
      protocol: 'consumer',
      state: 'STABLE',
      members: [
        {
          memberId: 'member-1',
          assignments: [
            { topicId: 'topic-2', topicName: 'payments', partition: 0 },
          ],
        },
      ],
      offsets: [
        {
          topicId: 'topic-2',
          topicName: 'payments',
          partition: 0,
          lag: 0,
          offset: 500,
          logEndOffset: 500,
        },
      ],
    },
  },
  {
    id: 'group-3',
    meta: { describeAvailable: false },
    attributes: {
      groupId: 'connect-sink',
      type: 'Classic',
      protocol: 'sink',
      state: 'DEAD',
      members: undefined,
      offsets: undefined,
    },
  },
] as unknown as Group[];

// Helper so each story only states what differs
const makeResult = (overrides: Record<string, unknown> = {}) =>
  ({
    data: { data: groups },
    isLoading: false,
    isPending: false,
    isError: false,
    isSuccess: true,
    error: null,
    status: 'success',
    fetchStatus: 'idle',
    ...overrides,
  } as unknown as UseQueryResult<ListResponse<Group>, Error>);

const meta = {
  component: GroupsDataView,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/kafka/kafka-1/groups']}>
        <div
          onClickCapture={(e) => {
            if ((e.target as HTMLElement).closest('a')) e.preventDefault();
          }}
        >
          <Story />
        </div>
      </MemoryRouter>
    ),
  ],
  args: {
    groupsResult: makeResult(),
    onDataViewChange: fn(),
    onResetOffset: fn(),
  },
} satisfies Meta<typeof GroupsDataView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: { groupsResult: makeResult({ data: { data: [] } }) },
};

export const Loading: Story = {
  args: {
    groupsResult: makeResult({
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
    groupsResult: makeResult({
      data: undefined,
      isError: true,
      isSuccess: false,
      status: 'error',
      error: new Error('Failed to load groups'),
    }),
  },
};
