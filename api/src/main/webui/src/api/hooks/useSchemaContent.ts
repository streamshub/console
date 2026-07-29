/**
 * Hook for fetching schema content from a schema registry link.
 *
 * The `links.content` field on a RelatedSchema is an absolute path from origin
 * (e.g. `/api/registries/{registryId}/schemas/{schemaId}`).
 * The response is plain text (the raw schema definition).
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';

export function useSchemaContent(contentUrl: string | null | undefined) {
  return useQuery({
    queryKey: ['schemaContent', contentUrl],
    queryFn: () => apiClient.getText(contentUrl!),
    enabled: !!contentUrl,
    staleTime: 5 * 60 * 1000,
  });
}
