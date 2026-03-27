/**
 * TanStack Query hooks for Consumer Groups
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { ConsumerGroupsResponse } from '../types';

/**
 * Fetch consumer groups for a specific topic
 */
export function useTopicConsumerGroups(
  kafkaId: string | undefined,
  topicId: string | undefined,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
  }
) {
  return useQuery({
    queryKey: [
      'topic-consumer-groups',
      kafkaId,
      topicId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
    ],
    queryFn: async () => {
      if (!kafkaId || !topicId) {
        throw new Error('Kafka ID and Topic ID are required');
      }

      const searchParams = new URLSearchParams();

      // Set default fields for groups
      searchParams.set(
        'fields[groups]',
        'groupId,type,protocol,state,simpleConsumerGroup,members,offsets,coordinator,partitionAssignor'
      );

      if (params?.pageSize) {
        searchParams.set('page[size]', params.pageSize.toString());
      }

      if (params?.pageCursor) {
        if (params.pageCursor.startsWith('after:')) {
          searchParams.set('page[after]', params.pageCursor.slice(6));
        } else if (params.pageCursor.startsWith('before:')) {
          searchParams.set('page[before]', params.pageCursor.slice(7));
        }
      }

      if (params?.sort) {
        const sortPrefix = params.sortDir === 'desc' ? '-' : '';
        searchParams.set('sort', `${sortPrefix}${params.sort}`);
      }

      const path = `/api/kafkas/${kafkaId}/topics/${topicId}/groups?${searchParams}`;

      return apiClient.get<ConsumerGroupsResponse>(path);
    },
    enabled: !!kafkaId && !!topicId,
    refetchInterval: 5000, // Auto-refresh every 5 seconds like Next.js version
  });
}