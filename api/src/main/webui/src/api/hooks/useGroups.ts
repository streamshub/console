/**
 * TanStack Query hooks for Groups
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';
import {
  GroupsResponse,
  Group,
  OffsetResetRequest,
  ApiResponse,
} from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

/**
 * Fetch groups for a Kafka cluster
 */
export function useGroups(
  kafkaId: string | undefined,
  params?: ResourceListParams
) {
  return useResourceList<Group>(
    'groups',
    `/api/kafkas/${kafkaId}/groups`,
    {
      ...params,
      fields: params?.fields ?? 'groupId,type,protocol,state,simpleConsumerGroup,members,offsets',
      enabled: !!kafkaId && (params?.enabled ?? true),
    }
  );
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
  });
}

/**
 * Hook for resetting consumer group offsets
 */
export function useResetGroupOffsets(kafkaId: string, groupId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      offsets,
      dryRun = false,
    }: {
      offsets: OffsetResetRequest[];
      dryRun?: boolean;
    }) => {
      const response = await apiClient.patch<ApiResponse<Group>>(
        `/api/kafkas/${kafkaId}/groups/${encodeURIComponent(groupId)}`,
        {
          meta: { dryRun },
          data: {
            type: 'groups',
            id: groupId,
            attributes: { offsets },
          },
        }
      );
      return response;
    },
    onSuccess: (_data, variables) => {
      if (!variables.dryRun) {
        // Invalidate group queries to refresh data after successful reset
        queryClient.invalidateQueries({ queryKey: ['group', kafkaId, groupId] });
        queryClient.invalidateQueries({ queryKey: ['groups', kafkaId] });
      }
    },
  });
}