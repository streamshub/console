/**
 * TanStack Query hook for Topic Metrics
 *
 * Fetches metrics data for a specific Kafka topic.
 * Supports time range filtering and auto-refresh.
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { TopicMetricsResponse } from '../types';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

export interface UseTopicMetricsParams {
  kafkaId: string | undefined;
  topicId: string | undefined;
  duration?: DurationOptions; // Duration in minutes (default: Last1hour = 60 minutes)
  enabled?: boolean;
  refetchInterval?: number | false; // Auto-refresh interval in ms
}

/**
 * Fetch metrics for a specific Kafka topic
 */
export function useTopicMetrics({
  kafkaId,
  topicId,
  duration = DurationOptions.Last1hour,
  enabled = true,
  refetchInterval = false,
}: UseTopicMetricsParams) {
  return useQuery({
    queryKey: ['topic-metrics', kafkaId, topicId, duration],
    queryFn: async () => {
      if (!kafkaId || !topicId) {
        throw new Error('kafkaId and topicId are required');
      }

      const searchParams = new URLSearchParams({
        'duration[metrics]': String(duration),
      });

      const path = `/api/kafkas/${kafkaId}/topics/${topicId}/metrics?${searchParams.toString()}`;
      
      return apiClient.get<TopicMetricsResponse>(path);
    },
    enabled: enabled && !!kafkaId && !!topicId,
    refetchInterval,
    // Keep previous data while fetching new data
    placeholderData: (previousData) => previousData,
  });
}

/**
 * Helper function to extract time series data from topic metrics
 */
export function extractTopicTimeSeriesData(
  metrics: TopicMetricsResponse['data']['attributes']['metrics'],
  metricName: string
): Array<{ timestamp: string; value: number }> {
  if (!metrics?.ranges?.[metricName]) {
    return [];
  }

  const ranges = metrics.ranges[metricName];
  
  // Flatten all ranges into a single time series
  const allPoints: Array<{ timestamp: string; value: number }> = [];
  
  ranges.forEach((range) => {
    range.range.forEach(([timestamp, value]) => {
      allPoints.push({
        timestamp,
        value: parseFloat(value),
      });
    });
  });

  // Sort by timestamp
  return allPoints.sort((a, b) => 
    new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  );
}

/**
 * Helper to get current metric value (latest value from the values object)
 */
export function getCurrentMetricValue(
  metrics: TopicMetricsResponse['data']['attributes']['metrics'],
  metricName: string
): number | null {
  if (!metrics?.values?.[metricName]?.[0]) {
    return null;
  }

  const value = metrics.values[metricName][0].value;
  return parseFloat(value);
}