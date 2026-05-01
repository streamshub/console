/**
 * TanStack Query hooks for Kafka Clusters
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';
import { ApiResponse, KafkaCluster, KafkaClustersResponse } from '../types';

/**
 * Fetch all Kafka clusters
 */
export function useKafkaClusters(params?: {
  pageSize?: number;
  pageCursor?: string;
  sort?: string;
  sortDir?: 'asc' | 'desc';
  name?: string;
}) {
  return useQuery({
    queryKey: [
      'kafka-clusters',
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.name,
    ],
    queryFn: async () => {
      const searchParams = new URLSearchParams();
      
      if (params?.pageSize) {
        searchParams.set('page[size]', String(params.pageSize));
      }
      
      // Handle cursor-based pagination
      if (params?.pageCursor) {
        if (params.pageCursor.startsWith('after:')) {
          searchParams.set('page[after]', params.pageCursor.slice(6));
        } else if (params.pageCursor.startsWith('before:')) {
          searchParams.set('page[before]', params.pageCursor.slice(7));
        }
      }

      // Handle sorting
      if (params?.sort) {
        const sortPrefix = params.sortDir === 'desc' ? '-' : '';
        searchParams.set('sort', `${sortPrefix}${params.sort}`);
      }

      // Handle name filter
      if (params?.name) {
        searchParams.set('filter[name]', `like,*${params.name}*`);
      }

      const queryString = searchParams.toString();
      const path = `/api/kafkas${queryString ? `?${queryString}` : ''}`;
      
      return apiClient.get<KafkaClustersResponse>(path);
    },
  });
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
    refetchInterval: 30000,
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