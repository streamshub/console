/**
 * TanStack Query hooks for Topics
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { TopicsResponse, Topic } from '../types';

/**
 * Fetch all topics for a Kafka cluster
 */
export function useTopics(
  kafkaId: string | undefined,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
    status?: string[];
    id?: string;
    includeHidden?: boolean;
    fields?: string[];
  }
) {
  return useQuery({
    queryKey: [
      'topics',
      kafkaId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.name,
      params?.status,
      params?.id,
      params?.includeHidden,
      params?.fields,
    ],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('Kafka ID is required');
      }

      const searchParams = new URLSearchParams();

      if (params?.pageSize) {
        searchParams.set('page[size]', params.pageSize.toString());
      }

      // Handle cursor-based pagination
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
      if (params?.name) {
        searchParams.set('filter[name]', `like,*${params.name}*`);
      }
      if (params?.status && params.status.length > 0) {
        searchParams.set('filter[status]', `in,${params.status.join(',')}`);
      }
      if (params?.id) {
        searchParams.set('filter[id]', params.id);
      }
      if (params?.includeHidden) {
        searchParams.set('filter[visibility]', 'in,internal,external');
      }
      if (params?.fields) {
        searchParams.set('fields[topics]', params.fields.join(','));
      }

      const queryString = searchParams.toString();
      const path = `/api/kafkas/${kafkaId}/topics${queryString ? `?${queryString}` : ''}`;

      return apiClient.get<TopicsResponse>(path);
    },
    enabled: !!kafkaId,
  });
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