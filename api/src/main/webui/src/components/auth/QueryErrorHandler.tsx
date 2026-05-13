import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useKafkaAuthContext } from './KafkaAuthProvider';

/**
 * Component that listens to TanStack Query errors and handles 401 responses
 * by showing the login modal
 */
export function QueryErrorHandler() {
  const queryClient = useQueryClient();
  const { handleAuthError } = useKafkaAuthContext();

  useEffect(() => {
    // Set up a global error handler for queries
    const unsubscribe = queryClient.getQueryCache().subscribe((event) => {
      if (event.type === 'updated' && event.action.type === 'error') {
        const error = event.action.error;
        
        // Try to extract clusterId from the query key
        // Query keys typically follow pattern: ['resource-name', clusterId, ...otherParams]
        const queryKey = event.query.queryKey;
        let clusterId: string | undefined;
        
        // Check if the second element in the query key is a cluster ID
        if (Array.isArray(queryKey) && queryKey.length > 1 && typeof queryKey[1] === 'string') {
          // Common patterns: ['kafka-cluster', clusterId], ['topics', clusterId], etc.
          clusterId = queryKey[1];
        }
        
        // Handle the auth error
        handleAuthError(error, clusterId);
      }
    });

    return () => {
      unsubscribe();
    };
  }, [queryClient, handleAuthError]);

  // This component doesn't render anything
  return null;
}