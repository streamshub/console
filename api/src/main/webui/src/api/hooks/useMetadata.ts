/**
 * TanStack Query hook for Metadata
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { MetadataResponse } from '../types';

/**
 * Fetch metadata (version, platform info)
 */
export function useMetadata() {
  return useQuery({
    queryKey: ['metadata'],
    queryFn: () => apiClient.get<MetadataResponse>('/api/metadata'),
    staleTime: 60 * 1000, // 60 seconds
  });
}