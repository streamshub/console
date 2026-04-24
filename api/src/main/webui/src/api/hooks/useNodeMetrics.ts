/**
 * TanStack Query hook for Node Metrics
 *
 * Fetches metrics data for a specific Kafka node (broker/controller).
 * Supports time range filtering and auto-refresh.
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { NodeMetricsResponse } from '../types';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

export interface UseNodeMetricsParams {
  kafkaId: string | undefined;
  nodeId: string | number | undefined;
  duration?: DurationOptions; // Duration in minutes (default: Last1hour = 60 minutes)
  enabled?: boolean;
  refetchInterval?: number | false; // Auto-refresh interval in ms
}

/**
 * Fetch metrics for a specific Kafka node
 */
export function useNodeMetrics({
  kafkaId,
  nodeId,
  duration = DurationOptions.Last1hour,
  enabled = true,
  refetchInterval = false,
}: UseNodeMetricsParams) {
  return useQuery({
    queryKey: ['node-metrics', kafkaId, nodeId, duration],
    queryFn: async () => {
      if (!kafkaId || nodeId === undefined) {
        throw new Error('kafkaId and nodeId are required');
      }

      const searchParams = new URLSearchParams({
        'duration[metrics]': String(duration),
      });

      const path = `/api/kafkas/${kafkaId}/nodes/${nodeId}/metrics?${searchParams.toString()}`;
      
      return apiClient.get<NodeMetricsResponse>(path);
    },
    enabled: enabled && !!kafkaId && nodeId !== undefined,
    refetchInterval,
    // Keep previous data while fetching new data
    placeholderData: (previousData) => previousData,
  });
}

/**
 * Helper function to extract time series data from metrics
 */
export function extractTimeSeriesData(
  metrics: NodeMetricsResponse['data']['attributes']['metrics'],
  metricName: string,
  nodeId?: string
): Array<{ timestamp: string; value: number }> {
  if (!metrics?.ranges?.[metricName]) {
    return [];
  }

  const ranges = metrics.ranges[metricName];
  
  // Filter by nodeId if specified
  const filteredRanges = nodeId
    ? ranges.filter((r) => r.nodeId === nodeId)
    : ranges;

  // Flatten all ranges into a single time series
  const allPoints: Array<{ timestamp: string; value: number }> = [];
  
  filteredRanges.forEach((range) => {
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