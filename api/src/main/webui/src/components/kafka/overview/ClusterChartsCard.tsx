/**
 * Cluster Charts Card Component
 *
 * Displays cluster-level metrics charts:
 * - Disk Usage
 * - CPU Usage
 * - Memory Usage
 *
 * Each chart supports broker filtering and time range selection.
 */

import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  CardTitle,
  EmptyState,
  EmptyStateBody,
  Title,
  Stack,
  StackItem,
  Flex,
  FlexItem,
  Divider,
  Tooltip,
} from '@patternfly/react-core';
import { ChartLineIcon, HelpIcon } from '@patternfly/react-icons';
import { useNodeMetrics } from '@/api/hooks/useNodeMetrics';
import { useKafkaMetrics } from '@/api/hooks/useKafkaMetrics';
import { ChartDiskUsage } from './charts/ChartDiskUsage';
import { ChartCpuUsage } from './charts/ChartCpuUsage';
import { ChartMemoryUsage } from './charts/ChartMemoryUsage';
import { FilterByBroker } from './filters/FilterByBroker';
import { FilterByTime } from './filters/FilterByTime';
import { ChartSkeletonLoader } from './ChartSkeletonLoader';
import { DurationOptions, TimeSeriesMetrics } from './utils/types';
import { timeSeriesMetrics } from './utils/metricsHelpers';

export interface ClusterChartsCardProps {
  kafkaId: string;
  nodes: Array<{ id: number; name: string }>;
  isLoading: boolean;
  metricsAvailable?: boolean;
}

export function ClusterChartsCard({
  kafkaId,
  nodes,
  isLoading,
  metricsAvailable = true,
}: ClusterChartsCardProps) {
  const { t } = useTranslation();
  const [selectedBroker, setSelectedBroker] = useState<string | null>(null);
  const [selectedDuration, setSelectedDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes
  );

  // Fetch cluster-level metrics when "All brokers" is selected
  const clusterMetrics = useKafkaMetrics({
    kafkaId,
    duration: selectedDuration,
    enabled: metricsAvailable && !isLoading && selectedBroker === null,
    refetchInterval: 30000,
  });

  // Fetch individual node metrics when a specific broker is selected
  const nodeMetrics = useNodeMetrics({
    kafkaId,
    nodeId: selectedBroker || undefined,
    duration: selectedDuration,
    enabled: metricsAvailable && !isLoading && selectedBroker !== null,
    refetchInterval: 30000,
  });

  const isLoadingMetrics = selectedBroker === null ? clusterMetrics.isLoading : nodeMetrics.isLoading;
  const hasError = selectedBroker === null ? clusterMetrics.isError : nodeMetrics.isError;

  // Extract metrics data for charts
  const diskUsageData = useMemo(() => {
    if (isLoadingMetrics || hasError) return {};
    
    const usages: Record<string, TimeSeriesMetrics> = {};
    const available: Record<string, TimeSeriesMetrics> = {};
    
    // Use cluster metrics when "All brokers" selected, otherwise use node metrics
    const metricsData = selectedBroker === null
      ? clusterMetrics.data?.data?.attributes?.metrics
      : nodeMetrics.data?.data?.attributes?.metrics;
    
    if (metricsData) {
      // Extract disk usage metrics
      const usageSeries = timeSeriesMetrics(metricsData.ranges, 'volume_stats_used_bytes');
      const availableSeries = timeSeriesMetrics(metricsData.ranges, 'volume_stats_capacity_bytes');
      
      Object.assign(usages, usageSeries);
      Object.assign(available, availableSeries);
    }
    
    return { usages, available };
  }, [clusterMetrics.data, nodeMetrics.data, selectedBroker, isLoadingMetrics, hasError]);

  const cpuUsageData = useMemo(() => {
    if (isLoadingMetrics || hasError) return {};
    
    const usages: Record<string, TimeSeriesMetrics> = {};
    
    // Use cluster metrics when "All brokers" selected, otherwise use node metrics
    const metricsData = selectedBroker === null
      ? clusterMetrics.data?.data?.attributes?.metrics
      : nodeMetrics.data?.data?.attributes?.metrics;
    
    if (metricsData) {
      // Extract CPU usage metrics
      const cpuSeries = timeSeriesMetrics(metricsData.ranges, 'cpu_usage_seconds');
      Object.assign(usages, cpuSeries);
    }
    
    return usages;
  }, [clusterMetrics.data, nodeMetrics.data, selectedBroker, isLoadingMetrics, hasError]);

  const memoryUsageData = useMemo(() => {
    if (isLoadingMetrics || hasError) return {};
    
    const usages: Record<string, TimeSeriesMetrics> = {};
    
    // Use cluster metrics when "All brokers" selected, otherwise use node metrics
    const metricsData = selectedBroker === null
      ? clusterMetrics.data?.data?.attributes?.metrics
      : nodeMetrics.data?.data?.attributes?.metrics;
    
    if (metricsData) {
      // Extract memory usage metrics
      const memorySeries = timeSeriesMetrics(metricsData.ranges, 'memory_usage_bytes');
      Object.assign(usages, memorySeries);
    }
    
    return usages;
  }, [clusterMetrics.data, nodeMetrics.data, selectedBroker, isLoadingMetrics, hasError]);

  return (
    <Card component="div" isFullHeight>
      <CardTitle>{t('ClusterChartsCard.cluster_metrics')}</CardTitle>
      <CardBody>
        {!metricsAvailable ? (
          <EmptyState>
            <ChartLineIcon />
            <Title headingLevel="h4" size="lg">
              {t('ClusterChartsCard.data_unavailable')}
            </Title>
            <EmptyStateBody>
              {t('ClusterChartsCard.virtual_cluster_metrics_unavailable')}
            </EmptyStateBody>
          </EmptyState>
        ) : (
          <Stack hasGutter>
            {/* Filters */}
            <StackItem>
              <Flex spaceItems={{ default: 'spaceItemsSm' }}>
                <FlexItem>
                  <FilterByBroker
                    brokerIds={nodes.map((n) => String(n.id))}
                    value={selectedBroker}
                    onChange={setSelectedBroker}
                  />
                </FlexItem>
                <FlexItem>
                  <FilterByTime
                    value={selectedDuration}
                    onChange={setSelectedDuration}
                  />
                </FlexItem>
              </Flex>
            </StackItem>

            <StackItem>
              <Divider />
            </StackItem>

            {/* Charts */}
            {isLoadingMetrics ? (
              <>
                <StackItem>
                  <ChartSkeletonLoader />
                </StackItem>
                <StackItem>
                  <ChartSkeletonLoader />
                </StackItem>
                <StackItem>
                  <ChartSkeletonLoader />
                </StackItem>
              </>
            ) : hasError ? (
              <StackItem>
                <EmptyState>
                  <ChartLineIcon />
                  <Title headingLevel="h4" size="lg">
                    {t('ClusterChartsCard.error_loading_metrics')}
                  </Title>
                  <EmptyStateBody>
                    {t('ClusterChartsCard.error_loading_metrics_description')}
                  </EmptyStateBody>
                </EmptyState>
              </StackItem>
            ) : (
              <>
                <StackItem>
                  <b>
                    {t('ClusterChartsCard.used_disk_space')}{' '}
                    <Tooltip content={t('ClusterChartsCard.used_disk_space_tooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </b>
                </StackItem>
                <StackItem>
                  <ChartDiskUsage
                    usages={diskUsageData.usages || {}}
                    available={diskUsageData.available || {}}
                    duration={selectedDuration}
                  />
                </StackItem>

                <StackItem>
                  <Divider />
                </StackItem>

                <StackItem>
                  <b>
                    {t('ClusterChartsCard.cpu_usage')}{' '}
                    <Tooltip content={t('ClusterChartsCard.cpu_usage_tooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </b>
                </StackItem>
                <StackItem>
                  <ChartCpuUsage
                    usages={cpuUsageData}
                    duration={selectedDuration}
                  />
                </StackItem>

                <StackItem>
                  <Divider />
                </StackItem>

                <StackItem>
                  <b>
                    {t('ClusterChartsCard.memory_usage')}{' '}
                    <Tooltip content={t('ClusterChartsCard.memory_usage_tooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </b>
                </StackItem>
                <StackItem>
                  <ChartMemoryUsage
                    usages={memoryUsageData}
                    duration={selectedDuration}
                  />
                </StackItem>
              </>
            )}
          </Stack>
        )}
      </CardBody>
    </Card>
  );
}