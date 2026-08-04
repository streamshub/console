/**
 * TanStack Query hooks for Rebalances
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';
import {
  RebalanceResponse,
  Rebalance,
} from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

const REBALANCE_FIELDS = 'name,namespace,creationTimestamp,status,mode,brokers,optimizationResult,conditions';
const REBALANCE_DETAIL_FIELDS = `${REBALANCE_FIELDS},goals,optimizationProposal,sessionId`;

/**
 * Fetch all rebalances for a Kafka cluster.
 *
 * Filter keys (pass via params.filters):
 *   name   – string, matched with 'like'
 *   status – string array, matched with 'in'
 *   mode   – string array, matched with 'in'
 */
export function useRebalances(
  kafkaId: string | undefined,
  params?: ResourceListParams,
) {
  return useResourceList<Rebalance>(
    'kafkaRebalances',
    `/api/kafkas/${kafkaId}/rebalances`,
    {
      fields: REBALANCE_FIELDS,
      ...params,
      enabled: !!kafkaId && (params?.enabled ?? true),
    },
  );
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

      const path = `/api/kafkas/${kafkaId}/rebalances/${rebalanceId}?fields[kafkaRebalances]=${REBALANCE_DETAIL_FIELDS}`;

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