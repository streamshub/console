import type { Meta, StoryObj } from '@storybook/react';
import { MemoryRouter } from 'react-router';
import type { Resource, ListResponse } from '@/api/types';
import type { ResourceListParams } from '@/api/hooks/useResourceList';
import type {
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
} from './ResourceListDataView';
import { ResourceListDataView } from './ResourceListDataView';
import { DataViewTh } from '@patternfly/react-data-view/dist/cjs/DataViewTable';
import type { UseQueryResult } from '@tanstack/react-query';

interface DemoResource extends Resource {
  id: string;
  name: string;
  status: string;
}

const demoResources = [
  { id: '1', name: 'Resource Alpha', status: 'Ready' },
  { id: '2', name: 'Resource Beta', status: 'Pending' },
  { id: '3', name: 'Resource Gamma', status: 'Ready' },
];

const makeResult = (
  data = demoResources,
  options: {
    isLoading?: boolean;
    error?: Error | null;
  } = {}
) => {
  const response = {
    data,
    meta: {
      page: {
        total: data.length,
        pageNumber: 1,
      },
    },
    links: {},
  } as unknown as ListResponse<DemoResource>;

  return {
    data: options.isLoading || options.error ? undefined : response,
    error: options.error ?? null,
    isLoading: options.isLoading ?? false,
    isPending: options.isLoading ?? false,
    isError: Boolean(options.error),
    isSuccess: !options.isLoading && !options.error,
    isFetching: options.isLoading ?? false,
    status: options.error ? 'error' : options.isLoading ? 'pending' : 'success',
    fetchStatus: options.isLoading ? 'fetching' : 'idle',
  } as unknown as UseQueryResult<ListResponse<DemoResource>, Error>;
};

const columnProvider: {
  dependencies: unknown[];
  callback: ResourceListDataViewColumnMapper;
} = {
  dependencies: [],
  callback: () => [{ cell: 'Name' }, { cell: 'Status' }] as DataViewTh[],
};

const rowProvider: {
  dependencies: unknown[];
  callback: ResourceListDataViewRowMapper<DemoResource>;
} = {
  dependencies: [],
  callback: (resource) =>
    [{ cell: resource.name }, { cell: resource.status }] as never,
};

const meta: Meta<typeof ResourceListDataView<DemoResource>> = {
  component: ResourceListDataView,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/resources']}>
        <div style={{ padding: '24px' }}>
          <Story />
        </div>
      </MemoryRouter>
    ),
  ],
  args: {
    columnProvider,
    rowProvider,
    onDataViewChange: (_params: ResourceListParams) => {},
    ariaLabel: 'Resources',
    ouiaIdPrefix: 'storybook-resources',
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    resourceResult: makeResult(),
  },
};

export const Loading: Story = {
  args: {
    resourceResult: makeResult([], { isLoading: true }),
  },
};

export const Empty: Story = {
  args: {
    resourceResult: makeResult([]),
  },
};

export const ErrorState: Story = {
  args: {
    resourceResult: makeResult([], {
      error: new Error('Unable to load resources.'),
    }),
  },
};

export const WithTextFilter: Story = {
  args: {
    resourceResult: makeResult(),
    dataFilters: {
      name: {
        type: 'text',
        title: 'Name',
        placeholder: 'Search by name',
      },
    },
  },
};

export const WithCheckboxFilter: Story = {
  args: {
    resourceResult: makeResult(),
    dataFilters: {
      status: {
        type: 'checkbox',
        title: 'Status',
        placeholder: 'Filter by status',
        options: [
          { label: 'Ready', value: 'ready' },
          { label: 'Pending', value: 'pending' },
        ],
      },
    },
  },
};

export const WithToggleFilter: Story = {
  args: {
    resourceResult: makeResult(),
    dataFilters: {
      enabled: {
        type: 'toggle',
        title: 'Enabled',
        label: 'Show enabled resources',
        initialValue: false,
      },
    },
  },
};
