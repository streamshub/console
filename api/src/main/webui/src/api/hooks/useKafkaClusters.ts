/**
 * TanStack Query hooks for Kafka Clusters
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';
import { ApiResponse, KafkaCluster } from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

/**
 * Fetch all Kafka clusters
 */
export function useKafkaClusters(params?: ResourceListParams) {
  return useResourceList<KafkaCluster>('kafkas', '/api/kafkas', params);
}

/**
 * Fetch a single Kafka cluster by ID
 */
export function useKafkaCluster(kafkaId: string | undefined, params?: {
  fields?: string;
}) {
  return useQuery({
    queryKey: ['kafka-cluster', kafkaId, params?.fields],
    queryFn: () => {
      const searchParams = new URLSearchParams();
      
      if (params?.fields) {
        searchParams.set('fields', params.fields);
      }
      
      const queryString = searchParams.toString();
      const path = `/api/kafkas/${kafkaId}${queryString ? `?${queryString}` : ''}`;
      
      return apiClient.get<{ data: KafkaCluster }>(path);
    },
    enabled: !!kafkaId,
    refetchInterval: 10000,
  });
}

export function usePatchKafkaCluster(kafkaId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (reconciliationPaused?: boolean) =>
      apiClient.patch<ApiResponse<undefined>>(`/api/kafkas/${kafkaId}`, {
        data: {
          type: 'kafkas',
          id: kafkaId,
          meta: {
            reconciliationPaused,
          },
          attributes: {},
        },
      }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['kafka-cluster', kafkaId] }),
        queryClient.invalidateQueries({ queryKey: ['kafka-clusters'] }),
      ]);
    },
  });
}