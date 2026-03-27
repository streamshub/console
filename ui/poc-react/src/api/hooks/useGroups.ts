/**
 * TanStack Query hooks for Groups
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { GroupsResponse, Group, GroupState, GroupType } from '../types';

/**
 * Fetch groups for a Kafka cluster
 */
export function useGroups(
  kafkaId: string | undefined,
  params?: {
    fields?: string;
    id?: string;
    type?: GroupType[];
    groupState?: GroupState[];
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
  }
) {
  return useQuery({
    queryKey: [
      'groups',
      kafkaId,
      params?.fields,
      params?.id,
      params?.type,
      params?.groupState,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
    ],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('Kafka ID is required');
      }

      const searchParams = new URLSearchParams();

      // Set default fields for groups
      searchParams.set(
        'fields[groups]',
        params?.fields ?? 'groupId,type,protocol,state,simpleConsumerGroup,members,offsets'
      );

      if (params?.id) {
        searchParams.set('filter[id]', `like,*${params.id}*`);
      }

      if (params?.type && params.type.length > 0) {
        searchParams.set('filter[type]', `in,${params.type.join(',')}`);
      }

      if (params?.groupState && params.groupState.length > 0) {
        searchParams.set('filter[state]', `in,${params.groupState.join(',')}`);
      }

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

      const path = `/api/kafkas/${kafkaId}/groups?${searchParams}`;

      return apiClient.get<GroupsResponse>(path);
    },
    enabled: !!kafkaId,
    refetchInterval: 5000, // Auto-refresh every 5 seconds
  });
}

/**
 * Fetch groups for a specific topic
 */
export function useTopicGroups(
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
      'topic-groups',
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

      return apiClient.get<GroupsResponse>(path);
    },
    enabled: !!kafkaId && !!topicId,
    refetchInterval: 5000, // Auto-refresh every 5 seconds like Next.js version
  });
}

/**
 * Fetch a single group by ID
 */
export function useGroup(
  kafkaId: string | undefined,
  groupId: string | undefined,
  params?: {
    fields?: string;
  }
) {
  return useQuery({
    queryKey: ['group', kafkaId, groupId, params?.fields],
    queryFn: async () => {
      if (!kafkaId || !groupId) {
        throw new Error('Kafka ID and Group ID are required');
      }

      const searchParams = new URLSearchParams();

      // Set default fields for group details
      searchParams.set(
        'fields[groups]',
        params?.fields ?? 'groupId,type,protocol,state,simpleConsumerGroup,members,offsets,coordinator,partitionAssignor,configs'
      );

      const path = `/api/kafkas/${kafkaId}/groups/${encodeURIComponent(groupId)}?${searchParams}`;

      const response = await apiClient.get<{ data: Group }>(path);
      return response.data;
    },
    enabled: !!kafkaId && !!groupId,
    refetchInterval: 5000, // Auto-refresh every 5 seconds
  });
}