/**
 * Kafka Overview Page - Cluster dashboard
 *
 * Displays comprehensive overview of a Kafka cluster including:
 * - Cluster details and status
 * - Broker and consumer group counts
 * - Topics and partitions statistics
 * - Recently viewed topics
 * - Metrics charts (placeholder for now)
 */

import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PageSection, Title } from '@patternfly/react-core';
import { useKafkaCluster } from '../api/hooks/useKafkaClusters';
import { useTopics } from '../api/hooks/useTopics';
import { useGroups } from '../api/hooks/useGroups';
import { useNodes } from '../api/hooks/useNodes';
import { useViewedTopics } from '../api/hooks/useViewedTopics';
import { OverviewLayout } from '../components/Overview/OverviewLayout';
import { ClusterCard } from '../components/Overview/ClusterCard';
import { ClusterChartsCard } from '../components/Overview/ClusterChartsCard';
import { RecentTopicsCard } from '../components/Overview/RecentTopicsCard';
import { TopicsPartitionsCard } from '../components/Overview/TopicsPartitionsCard';
import { TopicChartsCard } from '../components/Overview/TopicChartsCard';

export function KafkaOverview() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  // Fetch cluster data
  const { data: clusterData, isLoading: clusterLoading } = useKafkaCluster(kafkaId);
  const cluster = clusterData?.data;

  // Fetch topics summary for partition counts
  const { data: topicsSummaryData, isLoading: topicsSummaryLoading } = useTopics(kafkaId, {
    pageSize: 1,
    fields: ['status'],
  });

  // Fetch all topics for topic charts filter
  const { data: allTopicsData } = useTopics(kafkaId, {
    includeHidden: true,
    fields: ['name,visibility'],
    sort: 'name',
    pageSize: 100,
  });

  // Fetch groups count
  const { data: groupsData, isLoading: groupsLoading } = useGroups(kafkaId, {
    fields: 'groupId',
    pageSize: 1,
  });

  // Get viewed topics from localStorage
  const { viewedTopics } = useViewedTopics(kafkaId);

  // Fetch nodes to get broker counts and for charts
  const { data: nodesData } = useNodes(kafkaId, {
    fields: ['roles', 'broker'],
    pageSize: 100,
  });

  // Calculate broker counts
  const brokersTotal = nodesData?.data?.filter(
    (node) => node.attributes.roles?.includes('broker')
  ).length ?? 0;

  // Count online brokers (those with Running status)
  const brokersOnline = nodesData?.data?.filter(
    (node) =>
      node.attributes.roles?.includes('broker') &&
      node.attributes.broker?.status === 'Running'
  ).length ?? brokersTotal; // Default to total if status not available

  // Get groups count from meta
  const groupsCount = groupsData?.meta?.page?.total;

  // Get topic statistics from meta.summary (if available)
  const topicsMeta = topicsSummaryData?.meta as any;
  const totalTopics = topicsMeta?.page?.total ?? 0;
  
  // Calculate partition statistics
  // Note: The API should return summary data, but we'll use safe defaults
  const totalPartitions = topicsMeta?.summary?.totalPartitions ?? 0;
  const fullyReplicated = topicsMeta?.summary?.statuses?.FullyReplicated ?? 0;
  const underReplicated = 
    (topicsMeta?.summary?.statuses?.UnderReplicated ?? 0) +
    (topicsMeta?.summary?.statuses?.PartiallyOffline ?? 0);
  const offline = topicsMeta?.summary?.statuses?.Offline ?? 0;

  // Check if metrics are available (virtual clusters don't have metrics)
  const isVirtualCluster = cluster?.meta?.kind === 'virtualkafkaclusters.kroxylicious.io';
  const metricsAvailable = !isVirtualCluster;

  // Prepare nodes list for charts
  const nodes = nodesData?.data
    ?.filter((node) => node.attributes.roles?.includes('broker'))
    .map((node) => ({
      id: parseInt(node.id, 10),
      name: `Node ${node.id}`,
    })) ?? [];

  // Prepare topics list for charts
  const topics = allTopicsData?.data?.map((topic) => ({
    id: topic.id,
    name: topic.attributes.name,
    isInternal: topic.attributes.visibility === 'internal',
  })) ?? [];

  const isLoading = clusterLoading || topicsSummaryLoading || groupsLoading;

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {cluster?.attributes.name || t('overview.title')}
        </Title>
      </PageSection>
      
      <OverviewLayout
        clusterCard={
          <ClusterCard
            cluster={cluster}
            groupsCount={groupsCount}
            brokersOnline={brokersOnline}
            brokersTotal={brokersTotal}
            isLoading={isLoading}
          />
        }
        clusterChartsCard={
          <ClusterChartsCard
            kafkaId={kafkaId || ''}
            nodes={nodes}
            isLoading={isLoading}
            metricsAvailable={metricsAvailable}
          />
        }
        recentTopicsCard={
          <RecentTopicsCard
            viewedTopics={viewedTopics}
            isLoading={false}
          />
        }
        topicsPartitionsCard={
          <TopicsPartitionsCard
            totalTopics={totalTopics}
            totalPartitions={totalPartitions}
            fullyReplicated={fullyReplicated}
            underReplicated={underReplicated}
            offline={offline}
            isLoading={isLoading}
          />
        }
        topicChartsCard={
          <TopicChartsCard
            kafkaId={kafkaId || ''}
            topics={topics}
            isLoading={isLoading}
            metricsAvailable={metricsAvailable}
            isVirtualKafkaCluster={isVirtualCluster}
          />
        }
      />
    </>
  );
}