/**
 * TanStack Query hook for Kafka Cluster Authentication
 */

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';

/**
 * Authenticate to a Kafka cluster using form authentication
 */
export function useKafkaAuth(clusterId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ username, password }: { username: string; password: string }) => {
      // POST to /api/kafkas/{clusterId}/session with form parameters u and p
      return apiClient.postForm<void>(`/api/kafkas/${clusterId}/session`, {
        u: username,
        p: password,
      });
    },
    onSuccess: () => {
      // Invalidate the session user query to refresh the user info in the masthead
      queryClient.invalidateQueries({ queryKey: ['session-user', clusterId] });
    },
  });
}