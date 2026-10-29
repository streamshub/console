import type { Meta, StoryObj } from '@storybook/react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { KafkaUser, ListResponse } from '@/api/types';
import { UsersDataView } from './UsersDataView';
import { MemoryRouter } from 'react-router';

const mockUsers = [
  {
    id: 'user-1',
    meta: { privileges: ['GET'] },
    attributes: {
      name: 'my-kafka-user',
      namespace: 'kafka-dev',
      creationTimestamp: '2026-10-20T10:30:00Z',
      username: 'my-user',
      authenticationType: 'SCRAM-SHA-512',
    },
  },
  {
    id: 'user-2',
    meta: { privileges: [] },
    attributes: {
      name: 'another-kafka-user',
      namespace: 'kafka-prod',
      creationTimestamp: '2026-10-21T12:00:00Z',
      username: 'another-user',
      authenticationType: 'TLS',
    },
  },
] as KafkaUser[];

const createListResponse = (users: KafkaUser[]) =>
  ({
    data: users,
    meta: { page: { total: users.length, pageNumber: 1 } },
    links: {},
  } as unknown as ListResponse<KafkaUser>);

const createUsersResult = (
  data?: ListResponse<KafkaUser>,
  error: Error | null = null,
  isLoading = false
) =>
  ({
    data,
    error,
    isLoading,
    isPending: isLoading,
    isError: !!error,
    isSuccess: !!data && !error,
    isFetching: isLoading,
    status: error ? 'error' : isLoading ? 'pending' : 'success',
    fetchStatus: isLoading ? 'fetching' : 'idle',
    refetch: () => Promise.resolve(),
  } as unknown as UseQueryResult<ListResponse<KafkaUser>, Error>);

const meta: Meta<typeof UsersDataView> = {
  component: UsersDataView,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/kafka/my-cluster/users']}>
        <Story />
      </MemoryRouter>
    ),
  ],
  args: {
    usersResult: createUsersResult(createListResponse(mockUsers)),
    onDataViewChange: () => {},
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Empty: Story = {
  args: { usersResult: createUsersResult(createListResponse([])) },
};

export const Loading: Story = {
  args: {
    usersResult: createUsersResult(undefined, null, true),
  },
};

export const ErrorState: Story = {
  args: {
    usersResult: createUsersResult(
      undefined,
      new Error('Failed to load Kafka users')
    ),
  },
};
