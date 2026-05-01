/**
 * Hook to access the showLearning configuration option
 * 
 * This hook fetches the showLearning setting from the metadata API endpoint.
 * The setting controls whether learning/documentation links are displayed
 * throughout the application.
 * 
 * @returns boolean - Whether to show learning links (defaults to true if not set)
 */

import { useMetadata } from '@/api/hooks/useMetadata';

export function useShowLearning(): boolean {
  const { data: metadata } = useMetadata();
  return metadata?.data?.attributes?.options?.showLearning ?? true;
}