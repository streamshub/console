/**
 * TanStack Query hooks for Topics
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { Topic } from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

/**
 * Fetch all topics for a Kafka cluster
 */
export function useTopics(
  kafkaId: string | undefined,
  params?: ResourceListParams
) {
  return useResourceList<Topic>(
    'topics',
    `/api/kafkas/${kafkaId}/topics`,
    {
      ...params,
      enabled: !!kafkaId && (params?.enabled ?? true),
    }
  );
}

/**
 * Fetch a single topic by ID
 */
export function useTopic(
  kafkaId: string | undefined,
  topicId: string | undefined,
  params?: {
    fields?: string[];
  }
) {
  return useQuery({
    queryKey: ['topic', kafkaId, topicId, params?.fields],
    queryFn: async () => {
      if (!kafkaId || !topicId) {
        throw new Error('Kafka ID and Topic ID are required');
      }

      const searchParams = new URLSearchParams();

      const defaultFields =
        'name,status,visibility,partitions,numPartitions,authorizedOperations,configs,totalLeaderLogBytes,groups';
      searchParams.set('fields[topics]', params?.fields?.join(',') || defaultFields);

      const path = `/api/kafkas/${kafkaId}/topics/${topicId}?${searchParams}`;

      return apiClient.get<{ data: Topic }>(path);
    },
    enabled: !!kafkaId && !!topicId,
  });
}