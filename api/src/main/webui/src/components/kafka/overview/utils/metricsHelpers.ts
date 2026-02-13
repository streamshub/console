/**
 * Helper functions for transforming metrics data for charts
 */

import { Metrics } from '@/api/types';
import { TimeSeriesMetrics } from './types';

/**
 * Extract time series metrics from the metrics ranges object
 * Groups metrics by nodeId
 */
export function timeSeriesMetrics(
  ranges: Metrics['ranges'] | undefined,
  rangeName: string
): Record<string, TimeSeriesMetrics> {
  const series: Record<string, TimeSeriesMetrics> = {};

  if (ranges && ranges[rangeName]) {
    ranges[rangeName].forEach((r) => {
      const nodeId = r.nodeId || 'default';
      series[nodeId] = r.range.reduce(
        (acc, [timestamp, value]) => ({
          ...acc,
          [timestamp]: parseFloat(value),
        }),
        {} as TimeSeriesMetrics
      );
    });
  }

  return series;
}

/**
 * Extract single time series (no nodeId grouping) for topic metrics
 */
export function singleTimeSeriesMetrics(
  ranges: Metrics['ranges'] | undefined,
  rangeName: string
): TimeSeriesMetrics {
  if (!ranges || !ranges[rangeName] || ranges[rangeName].length === 0) {
    return {};
  }

  const firstRange = ranges[rangeName][0];
  return firstRange.range.reduce(
    (acc, [timestamp, value]) => ({
      ...acc,
      [timestamp]: parseFloat(value),
    }),
    {} as TimeSeriesMetrics
  );
}