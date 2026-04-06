/**
 * TanStack Query hooks for Rebalances
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';
import {
  RebalancesResponse,
  RebalanceResponse,
  RebalanceStatus,
  RebalanceMode,
} from '../types';

/**
 * Fetch all rebalances for a Kafka cluster
 */
export function useRebalances(
  kafkaId: string | undefined,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
    status?: RebalanceStatus[];
    mode?: RebalanceMode[];
  }
) {
  return useQuery({
    queryKey: [
      'rebalances',
      kafkaId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.name,
      params?.status,
      params?.mode,
    ],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('Kafka ID is required');
      }

      const searchParams = new URLSearchParams();

      // Always include these fields
      searchParams.set(
        'fields[kafkaRebalances]',
        'name,namespace,creationTimestamp,status,mode,brokers,optimizationResult,conditions'
      );

      if (params?.pageSize) {
        searchParams.set('page[size]', params.pageSize.toString());
      }

      // Handle cursor-based pagination
      if (params?.pageCursor) {
        searchParams.set('page[after]', params.pageCursor);
      }

      if (params?.sort) {
        const sortPrefix = params.sortDir === 'desc' ? '-' : '';
        searchParams.set('sort', `${sortPrefix}${params.sort}`);
      }

      if (params?.name) {
        searchParams.set('filter[name]', `like,*${params.name}*`);
      }

      if (params?.status && params.status.length > 0) {
        searchParams.set('filter[status]', `in,${params.status.join(',')}`);
      }

      if (params?.mode && params.mode.length > 0) {
        searchParams.set('filter[mode]', `in,${params.mode.join(',')}`);
      }

      const queryString = searchParams.toString();
      const path = `/api/kafkas/${kafkaId}/rebalances${queryString ? `?${queryString}` : ''}`;

      return apiClient.get<RebalancesResponse>(path);
    },
    enabled: !!kafkaId,
  });
}

/**
 * Fetch a single rebalance by ID
 */
export function useRebalance(
  kafkaId: string | undefined,
  rebalanceId: string | undefined
) {
  return useQuery({
    queryKey: ['rebalance', kafkaId, rebalanceId],
    queryFn: async () => {
      if (!kafkaId || !rebalanceId) {
        throw new Error('Kafka ID and Rebalance ID are required');
      }

      const path = `/api/kafkas/${kafkaId}/rebalances/${rebalanceId}`;

      return apiClient.get<RebalanceResponse>(path);
    },
    enabled: !!kafkaId && !!rebalanceId,
  });
}

/**
 * Patch a rebalance (approve, stop, refresh)
 */
export function usePatchRebalance(kafkaId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      rebalanceId,
      action,
    }: {
      rebalanceId: string;
      action: 'approve' | 'stop' | 'refresh';
    }) => {
      const path = `/api/kafkas/${kafkaId}/rebalances/${rebalanceId}`;

      return apiClient.patch<RebalanceResponse>(path, {
        data: {
          type: 'kafkaRebalances',
          id: decodeURIComponent(rebalanceId),
          meta: {
            action: action,
          },
          attributes: {},
        },
      });
    },
    onSuccess: () => {
      // Invalidate rebalances queries to refetch
      queryClient.invalidateQueries({ queryKey: ['rebalances', kafkaId] });
    },
  });
}