/**
 * TanStack Query hook for Console User
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';

/**
 * Fetch a single Kafka user by ID
 */
export function useSessionUser() {
  return useQuery({
    queryKey: ['session-user'],
    queryFn: async () => {
      const path = `/api/session/user`;

      return apiClient.get<{
        username: string,
        fullName?: string,
        anonymous: boolean,
      }>(path);
    },
    staleTime: 1000 * 60, // 1 minute
  });
}
