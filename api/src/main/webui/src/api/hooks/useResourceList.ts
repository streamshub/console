/**
 * TanStack Query hooks for generic resource lists
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { ListResponse, Resource } from '../types';

export interface ResourceListPageParams {
  size?: number | null;
  beforeCursor?: string | null;
  afterCursor?: string | null;
  sort?: {
    field: string;
    direction?: 'asc' | 'desc';
  } | string;
}

export interface ResourceListParams {
  /**
   * Parameters for pagination (page size, sorting, etc.)
   */
  page?: ResourceListPageParams;
  /**
   * Parameters for filtering by specific fields (search, etc.)
   */
  filters?: Record<string, string | string[]>;
  /**
   * Comma-separated list of fields to include in the response.
   * @example
   * fields=name,status
   */
  fields?: string;

  /**
   * Whether the query should be enabled or not.
   * @default true
   */
  enabled?: boolean;

  /**
   * If set, the query will continuously refetch at this frequency in milliseconds.
   */
  refreshInterval?: number;
}

function updatePageParams(page: ResourceListPageParams, searchParams: URLSearchParams) {
  if (page.size) {
    searchParams.set('page[size]', String(page.size));
  }
  
  // Handle cursor-based pagination
  if (page.afterCursor) {
    searchParams.set('page[after]', page.afterCursor);
  } else if (page.beforeCursor) {
    searchParams.set('page[before]', page.beforeCursor);
  }

  // Handle sorting
  if (page.sort) {
    if (typeof page.sort === 'string') {
      searchParams.set('sort', page.sort);
    } else {
      const sortPrefix = page.sort.direction === 'desc' ? '-' : '';
      searchParams.set('sort', `${sortPrefix}${page.sort.field}`);
    }
  }
}

export function useResourceList<T extends Resource>(
  resourceType: string,
  path: string,
  params?: ResourceListParams,
) {
  return useQuery({
    queryKey: [
      resourceType + '-resource-list-query',
      JSON.stringify(params),
    ],
    queryFn: async () => {
      const searchParams = new URLSearchParams();

      if (params?.page) {
        updatePageParams(params.page, searchParams);
      }

      // Handle name filter
      if (params?.filters) {
        Object.entries(params.filters).forEach(([key, value]) => {
          searchParams.set(`filter[${key}]`, `like,*${value}*`);
        });
      }

      if (params?.fields) {
        searchParams.set(`fields[${resourceType}]`, params.fields);
      }

      const queryString = searchParams.toString();
      const url = path + (queryString ? `?${queryString}` : '');
      return apiClient.get<ListResponse<T>>(url);
    },
    enabled: params?.enabled,
    refetchInterval: params?.refreshInterval,
    placeholderData: (previousData) => previousData, // Keep previous data while loading new page
  });
}
