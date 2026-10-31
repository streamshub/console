/**
 * Topic Charts Card Component
 *
 * Displays topic-level metrics charts:
 * - Incoming/Outgoing bytes rate
 *
 * Supports topic filtering and time range selection.
 *
 * Split in two:
 * - TopicChartsCard      -> state + data fetching (hooks, useMemo)
 * - TopicChartsCardView  -> pure UI, takes everything as props (used by Storybook)
 */

import { useState, useMemo } from 'react';
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
import { DurationOptions, TimeSeriesMetrics } from './utils/types';
import { singleTimeSeriesMetrics } from './utils/metricsHelpers';

type TopicItem = { id: string; name: string; isInternal?: boolean };

export interface TopicChartsCardProps {
  kafkaId: string;
  topics: Array<TopicItem>;
  isLoading: boolean;
  metricsAvailable?: boolean;
  isVirtualKafkaCluster?: boolean;
}

/* -------------------------------------------------------------------------- */
/* Container: state + data fetching                                            */
/* -------------------------------------------------------------------------- */

export function TopicChartsCard({
  kafkaId,
  topics,
  isLoading,
  metricsAvailable = true,
  isVirtualKafkaCluster = false,
}: TopicChartsCardProps) {
  const { t } = useTranslation();
  const allTopics = useMemo(
    () => [null, t('metrics.all_topics')] as [string | null, string],
    [t]
  );
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [selectedDuration, setSelectedDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes
  );
  const [showInternal, setShowInternal] = useState(false);

  // Filter topics based on showInternal setting
  const filteredTopics = useMemo(() => {
    return showInternal ? topics : topics.filter((topic) => !topic.isInternal);
  }, [topics, showInternal]);

  // Selected topic that is actually present in the filtered list
  const [validSelectedTopic, validSelectedTopicName] = useMemo(() => {
    if (!selectedTopic) {
      return allTopics;
    }

    const matchingTopic = filteredTopics.find(
      (topic) => topic.id === selectedTopic
    );

    if (matchingTopic) {
      return [matchingTopic.id, matchingTopic.name] as [string | null, string];
    } else {
      return allTopics;
    }
  }, [filteredTopics, selectedTopic, allTopics]);

  // Fetch cluster-level metrics when "All topics" is selected (no topic selected)
  const clusterMetrics = useKafkaMetrics({
    kafkaId,
    duration: selectedDuration,
    enabled: metricsAvailable && !isLoading && validSelectedTopic === null,
    refetchInterval: 30000,
  });

  // Fetch individual topic metrics when a specific topic is selected
  const topicMetrics = useTopicMetrics({
    kafkaId,
    topicId: validSelectedTopic || undefined,
    duration: selectedDuration,
    enabled: metricsAvailable && !isLoading && validSelectedTopic !== null,
    refetchInterval: 30000,
  });

  const isLoadingMetrics =
    validSelectedTopic === null
      ? clusterMetrics.isLoading
      : topicMetrics.isLoading;
  const isError =
    validSelectedTopic === null ? clusterMetrics.isError : topicMetrics.isError;

  // Extract incoming and outgoing metrics
  const { incoming, outgoing } = useMemo(() => {
    // Use cluster metrics when no topic selected, otherwise use topic metrics
    const metricsData =
      validSelectedTopic === null
        ? clusterMetrics.data?.data?.attributes?.metrics
        : topicMetrics.data?.data?.attributes?.metrics;

    if (!metricsData) {
      return { incoming: {}, outgoing: {} };
    }

    return {
      incoming: singleTimeSeriesMetrics(
        metricsData.ranges,
        'incoming_byte_rate'
      ),
      outgoing: singleTimeSeriesMetrics(
        metricsData.ranges,
        'outgoing_byte_rate'
      ),
    };
  }, [clusterMetrics.data, topicMetrics.data, validSelectedTopic]);

  return (
    <TopicChartsCardView
      filteredTopics={filteredTopics}
      hasTopics={topics.length > 0}
      metricsAvailable={metricsAvailable}
      isVirtualKafkaCluster={isVirtualKafkaCluster}
      showInternal={showInternal}
      onShowInternalChange={setShowInternal}
      selectedTopicId={validSelectedTopic}
      selectedTopicName={validSelectedTopicName}
      onTopicChange={setSelectedTopic}
      selectedDuration={selectedDuration}
      onDurationChange={setSelectedDuration}
      isLoadingMetrics={isLoadingMetrics}
      isError={isError}
      incoming={incoming}
      outgoing={outgoing}
    />
  );
}

/* -------------------------------------------------------------------------- */
/* View: pure UI                                                               */
/* -------------------------------------------------------------------------- */

export interface TopicChartsCardViewProps {
  filteredTopics: Array<TopicItem>;
  hasTopics: boolean;
  metricsAvailable: boolean;
  isVirtualKafkaCluster: boolean;
  showInternal: boolean;
  onShowInternalChange: (checked: boolean) => void;
  selectedTopicId: string | null;
  selectedTopicName: string | null;
  onTopicChange: (topicId: string | null) => void;
  selectedDuration: DurationOptions;
  onDurationChange: (duration: DurationOptions) => void;
  isLoadingMetrics: boolean;
  isError: boolean;
  incoming: TimeSeriesMetrics;
  outgoing: TimeSeriesMetrics;
}

export function TopicChartsCardView({
  filteredTopics,
  hasTopics,
  metricsAvailable,
  isVirtualKafkaCluster,
  showInternal,
  onShowInternalChange,
  selectedTopicId,
  selectedTopicName,
  onTopicChange,
  selectedDuration,
  onDurationChange,
  isLoadingMetrics,
  isError,
  incoming,
  outgoing,
}: TopicChartsCardViewProps) {
  const { t } = useTranslation();

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
        ) : !hasTopics ? (
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
                        {t('topics.showInternalTopics')}
                        &nbsp;
                        <Tooltip
                          content={t('topics.showInternalTopicsTooltip')}
                        >
                          <HelpIcon />
                        </Tooltip>
                      </>
                    }
                    isChecked={showInternal}
                    onChange={(_event, checked) =>
                      onShowInternalChange(checked)
                    }
                  />
                </FlexItem>
                <FlexItem>
                  <FilterByTopic
                    topics={filteredTopics}
                    value={selectedTopicId}
                    onChange={onTopicChange}
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
                  selectedTopicName={selectedTopicName ?? 'N/A'}
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
