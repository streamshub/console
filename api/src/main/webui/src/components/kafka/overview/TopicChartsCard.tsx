/**
 * Topic Charts Card Component
 *
 * Displays topic-level metrics charts:
 * - Incoming/Outgoing bytes rate
 *
 * Supports topic filtering and time range selection.
 */

import { useState, useMemo, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Content,
  EmptyState,
  EmptyStateBody,
  Title,
  Stack,
  StackItem,
  Flex,
  FlexItem,
  Divider,
  Switch,
  Tooltip,
} from '@patternfly/react-core';
import { ChartLineIcon, HelpIcon } from '@patternfly/react-icons';
import { useTopicMetrics } from '@/api/hooks/useTopicMetrics';
import { useKafkaMetrics } from '@/api/hooks/useKafkaMetrics';
import { ChartIncomingOutgoing } from './charts/ChartIncomingOutgoing';
import { FilterByTopic } from './filters/FilterByTopic';
import { FilterByTime } from './filters/FilterByTime';
import { ChartSkeletonLoader } from './ChartSkeletonLoader';
import { DurationOptions } from './utils/types';
import { singleTimeSeriesMetrics } from './utils/metricsHelpers';

export interface TopicChartsCardProps {
  kafkaId: string;
  topics: Array<{ id: string; name: string; isInternal?: boolean }>;
  isLoading: boolean;
  metricsAvailable?: boolean;
  isVirtualKafkaCluster?: boolean;
}

export function TopicChartsCard({
  kafkaId,
  topics,
  isLoading,
  metricsAvailable = true,
  isVirtualKafkaCluster = false,
}: TopicChartsCardProps) {
  const { t } = useTranslation();
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [selectedDuration, setSelectedDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes
  );
  const [hideInternal, setHideInternal] = useState(true);

  // Filter topics based on hideInternal setting
  const filteredTopics = useMemo(() => {
    return hideInternal
      ? topics.filter((t) => !t.isInternal)
      : topics;
  }, [topics, hideInternal]);

  // Reset selected topic if it's filtered out
  useEffect(() => {
    if (!selectedTopic) return;

    const existsInFiltered = filteredTopics.some(
      (t) => t.id === selectedTopic
    );

    if (!existsInFiltered) {
      setSelectedTopic(null);
    }
  }, [filteredTopics, selectedTopic]);

  // Fetch cluster-level metrics when "All topics" is selected (no topic selected)
  const clusterMetrics = useKafkaMetrics({
    kafkaId,
    duration: selectedDuration,
    enabled: metricsAvailable && !isLoading && selectedTopic === null,
    refetchInterval: 30000,
  });

  // Fetch individual topic metrics when a specific topic is selected
  const topicMetrics = useTopicMetrics({
    kafkaId,
    topicId: selectedTopic || undefined,
    duration: selectedDuration,
    enabled: metricsAvailable && !isLoading && selectedTopic !== null,
    refetchInterval: 30000,
  });

  const isLoadingMetrics = selectedTopic === null ? clusterMetrics.isLoading : topicMetrics.isLoading;
  const isError = selectedTopic === null ? clusterMetrics.isError : topicMetrics.isError;

  // Extract incoming and outgoing metrics
  const { incoming, outgoing } = useMemo(() => {
    // Use cluster metrics when no topic selected, otherwise use topic metrics
    const metricsData = selectedTopic === null
      ? clusterMetrics.data?.data?.attributes?.metrics
      : topicMetrics.data?.data?.attributes?.metrics;

    if (!metricsData) {
      return { incoming: {}, outgoing: {} };
    }
    
    return {
      incoming: singleTimeSeriesMetrics(metricsData.ranges, 'incoming_byte_rate'),
      outgoing: singleTimeSeriesMetrics(metricsData.ranges, 'outgoing_byte_rate'),
    };
  }, [clusterMetrics.data, topicMetrics.data, selectedTopic, selectedDuration]);

  const selectedTopicName = useMemo(() => {
    return topics.find((t) => t.id === selectedTopic)?.name;
  }, [topics, selectedTopic]);

  return (
    <Card component="div" isFullHeight>
      <CardHeader>
        <Content>
          <CardTitle>{t('topicMetricsCard.topic_metric')}</CardTitle>
          <Content component="small">
            {t('topicMetricsCard.topics_bytes_incoming_and_outgoing')}{' '}
            <Tooltip
              content={t(
                'topicMetricsCard.topics_bytes_incoming_and_outgoing_tooltip'
              )}
            >
              <HelpIcon />
            </Tooltip>
          </Content>
        </Content>
      </CardHeader>
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
        ) : topics.length === 0 ? (
          <EmptyState>
            <ChartLineIcon />
            <Title headingLevel="h4" size="lg">
              {t('topicMetricsCard.no_topics')}
            </Title>
            <EmptyStateBody>
              {t('topicMetricsCard.no_topics_description')}
            </EmptyStateBody>
          </EmptyState>
        ) : (
          <Stack hasGutter>
            {/* Filters */}
            <StackItem>
              <Flex spaceItems={{ default: 'spaceItemsSm' }}>
                <FlexItem>
                  <Switch
                    label={
                      <>
                        {t('topics.hideInternalTopics')}
                        &nbsp;
                        <Tooltip content={t('topics.hideInternalTopicsTooltip')}>
                          <HelpIcon />
                        </Tooltip>
                      </>
                    }
                    isChecked={hideInternal}
                    onChange={(_event, checked) => setHideInternal(checked)}
                  />
                </FlexItem>
                <FlexItem>
                  <FilterByTopic
                    topics={filteredTopics}
                    value={selectedTopic}
                    onChange={setSelectedTopic}
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

            {/* Chart */}
            {isLoadingMetrics ? (
              <StackItem>
                <ChartSkeletonLoader />
              </StackItem>
            ) : isError ? (
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
              <StackItem>
                <ChartIncomingOutgoing
                  incoming={incoming}
                  outgoing={outgoing}
                  isVirtualKafkaCluster={isVirtualKafkaCluster}
                  selectedTopicName={selectedTopicName}
                  duration={selectedDuration}
                />
              </StackItem>
            )}
          </Stack>
        )}
      </CardBody>
    </Card>
  );
}