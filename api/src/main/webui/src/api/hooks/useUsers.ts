/**
 * TanStack Query hooks for Kafka Users
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { UserResponse, KafkaUser } from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

/**
 * Fetch all Kafka users for a cluster
 */
export function useUsers(
  kafkaId: string | undefined,
  params?: ResourceListParams
) {
  return useResourceList<KafkaUser>(
    'kafkaUsers',
    `/api/kafkas/${kafkaId}/users`,
    {
      ...params,
      fields: params?.fields ?? 'name,namespace,creationTimestamp,username,authenticationType',
      enabled: !!kafkaId && (params?.enabled ?? true),
    }
  );
}

/**
 * Fetch a single Kafka user by ID
 */
export function useUser(
  kafkaId: string | undefined,
  userId: string | undefined,
  params?: {
    fields?: string[];
  }
) {
  return useQuery({
    queryKey: ['user', kafkaId, userId, params?.fields],
    queryFn: async () => {
      if (!kafkaId || !userId) {
        throw new Error('Kafka ID and User ID are required');
      }

      const searchParams = new URLSearchParams();

      const defaultFields =
        'name,namespace,creationTimestamp,username,authenticationType,authorization';
      searchParams.set('fields[kafkaUsers]', params?.fields?.join(',') || defaultFields);

      const path = `/api/kafkas/${kafkaId}/users/${userId}?${searchParams}`;

      return apiClient.get<UserResponse>(path);
    },
    enabled: !!kafkaId && !!userId,
  });
}