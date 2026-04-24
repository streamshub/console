/**
 * TanStack Query hooks for Kafka Users
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { UsersResponse, UserResponse } from '../types';

/**
 * Fetch all Kafka users for a cluster
 */
export function useUsers(
  kafkaId: string | undefined,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    username?: string;
    fields?: string[];
  }
) {
  return useQuery({
    queryKey: [
      'users',
      kafkaId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.username,
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

      if (params?.username) {
        searchParams.set('filter[username]', `like,*${params.username}*`);
      }

      if (params?.fields) {
        searchParams.set('fields[kafkaUsers]', params.fields.join(','));
      } else {
        // Default fields
        searchParams.set(
          'fields[kafkaUsers]',
          'name,namespace,creationTimestamp,username,authenticationType'
        );
      }

      const queryString = searchParams.toString();
      const path = `/api/kafkas/${kafkaId}/users${queryString ? `?${queryString}` : ''}`;

      return apiClient.get<UsersResponse>(path);
    },
    enabled: !!kafkaId,
  });
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