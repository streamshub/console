/**
 * TanStack Query hook for Kafka Cluster Metrics
 *
 * Fetches aggregated metrics data for the entire Kafka cluster.
 * Used when viewing metrics for all brokers combined.
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { Metrics } from '../types';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

export interface UseKafkaMetricsParams {
  kafkaId: string | undefined;
  duration?: DurationOptions; // Duration in minutes (default: Last1hour = 60 minutes)
  enabled?: boolean;
  refetchInterval?: number | false; // Auto-refresh interval in ms
}

export interface KafkaMetricsResponse {
  data: {
    id: string;
    type: 'kafkas';
    attributes: {
      name: string;
      metrics?: Metrics | null;
    };
  };
}

/**
 * Fetch aggregated metrics for the entire Kafka cluster
 */
export function useKafkaMetrics({
  kafkaId,
  duration = DurationOptions.Last1hour,
  enabled = true,
  refetchInterval = false,
}: UseKafkaMetricsParams) {
  return useQuery({
    queryKey: ['kafka-metrics', kafkaId, duration],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('kafkaId is required');
      }

      const searchParams = new URLSearchParams({
        'duration[metrics]': String(duration),
        'fields[kafkas]': 'metrics',
      });

      const path = `/api/kafkas/${kafkaId}?${searchParams.toString()}`;
      
      return apiClient.get<KafkaMetricsResponse>(path);
    },
    enabled: enabled && !!kafkaId,
    refetchInterval,
    // Keep previous data while fetching new data
    placeholderData: (previousData) => previousData,
  });
}