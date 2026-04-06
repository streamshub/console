/**
 * TanStack Query hooks for Kafka Clusters
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { KafkaCluster, KafkaClustersResponse } from '../types';

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
export function useKafkaCluster(kafkaId: string | undefined) {
  return useQuery({
    queryKey: ['kafka-cluster', kafkaId],
    queryFn: () => apiClient.get<{ data: KafkaCluster }>(`/api/kafkas/${kafkaId}`),
    enabled: !!kafkaId,
  });
}