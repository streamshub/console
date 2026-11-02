/**
 * Cluster Charts Card Component
 *
 * Displays cluster-level metrics charts:
 * - Disk Usage
 * - CPU Usage
 * - Memory Usage
 *
 * Split in two:
 * - ClusterChartsCard      -> fetches the metrics (hooks, useMemo)
 * - ClusterChartsCardView  -> pure UI, takes everything as props (used by Storybook)
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

/* -------------------------------------------------------------------------- */
/* Container: data fetching                                                    */
/* -------------------------------------------------------------------------- */

export function ClusterChartsCard({
  kafkaId,
  nodes,
  isLoading,
  metricsAvailable = true,
}: ClusterChartsCardProps) {
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

  const isLoadingMetrics =
    selectedBroker === null ? clusterMetrics.isLoading : nodeMetrics.isLoading;
  const hasError =
    selectedBroker === null ? clusterMetrics.isError : nodeMetrics.isError;

  // Use cluster metrics when "All brokers" selected, otherwise use node metrics
  const metricsData =
    selectedBroker === null
      ? clusterMetrics.data?.data?.attributes?.metrics
      : nodeMetrics.data?.data?.attributes?.metrics;

  const diskUsageData = useMemo(() => {
    if (isLoadingMetrics || hasError || !metricsData) return {};
    return {
      usages: timeSeriesMetrics(metricsData.ranges, 'volume_stats_used_bytes'),
      available: timeSeriesMetrics(
        metricsData.ranges,
        'volume_stats_capacity_bytes'
      ),
    };
  }, [metricsData, isLoadingMetrics, hasError]);

  const cpuUsageData = useMemo(() => {
    if (isLoadingMetrics || hasError || !metricsData) return {};
    return timeSeriesMetrics(metricsData.ranges, 'cpu_usage_seconds');
  }, [metricsData, isLoadingMetrics, hasError]);

  const memoryUsageData = useMemo(() => {
    if (isLoadingMetrics || hasError || !metricsData) return {};
    return timeSeriesMetrics(metricsData.ranges, 'memory_usage_bytes');
  }, [metricsData, isLoadingMetrics, hasError]);

  return (
    <ClusterChartsCardView
      nodes={nodes}
      metricsAvailable={metricsAvailable}
      selectedBroker={selectedBroker}
      onBrokerChange={setSelectedBroker}
      selectedDuration={selectedDuration}
      onDurationChange={setSelectedDuration}
      isLoadingMetrics={isLoadingMetrics}
      hasError={hasError}
      diskUsageData={diskUsageData}
      cpuUsageData={cpuUsageData}
      memoryUsageData={memoryUsageData}
    />
  );
}

/* -------------------------------------------------------------------------- */
/* View: pure UI                                                               */
/* -------------------------------------------------------------------------- */

export interface ClusterChartsCardViewProps {
  nodes: Array<{ id: number; name: string }>;
  metricsAvailable: boolean;
  selectedBroker: string | null;
  onBrokerChange: (broker: string | null) => void;
  selectedDuration: DurationOptions;
  onDurationChange: (duration: DurationOptions) => void;
  isLoadingMetrics: boolean;
  hasError: boolean;
  diskUsageData: {
    usages?: Record<string, TimeSeriesMetrics>;
    available?: Record<string, TimeSeriesMetrics>;
  };
  cpuUsageData: Record<string, TimeSeriesMetrics>;
  memoryUsageData: Record<string, TimeSeriesMetrics>;
}

export function ClusterChartsCardView({
  nodes,
  metricsAvailable,
  selectedBroker,
  onBrokerChange,
  selectedDuration,
  onDurationChange,
  isLoadingMetrics,
  hasError,
  diskUsageData,
  cpuUsageData,
  memoryUsageData,
}: ClusterChartsCardViewProps) {
  const { t } = useTranslation();

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
                    onChange={onBrokerChange}
                  />
                </FlexItem>
                <FlexItem>
                  <FilterByTime
                    value={selectedDuration}
                    onChange={onDurationChange}
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
                    <Tooltip
                      content={t('ClusterChartsCard.used_disk_space_tooltip')}
                    >
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
                    <Tooltip
                      content={t('ClusterChartsCard.memory_usage_tooltip')}
                    >
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
