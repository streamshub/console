/**
 * Kafka Layout - Wrapper for Kafka cluster pages
 */

import { Outlet, useParams, useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Page,
  PageSection,
  EmptyState,
  EmptyStateBody,
  Spinner,
  Title,
  Breadcrumb,
  BreadcrumbItem,
} from '@patternfly/react-core';
import { HomeIcon } from '@patternfly/react-icons';
import { useKafkaCluster, useKafkaClusters } from '@/api/hooks/useKafkaClusters';
import { useConnector, useConnectCluster } from '@/api/hooks/useConnect';
import { useTopic } from '@/api/hooks/useTopics';
import { useUser } from '@/api/hooks/useUsers';
import { KafkaClusterSidebar } from '@/components/kafka/KafkaClusterSidebar';
import { AppMasthead } from '@/components/app/AppMasthead';
import { ReconciliationControls } from '@/components/kafka/overview/ReconciliationControls';

export function KafkaLayout() {
  const { t } = useTranslation();
  const {
    kafkaId,
    topicId,
    groupId,
    connectorId,
    connectClusterId,
    userId,
    nodeId
  } = useParams<{
    kafkaId: string;
    topicId?: string;
    groupId?: string;
    connectorId?: string;
    connectClusterId?: string;
    userId?: string;
    nodeId?: string;
  }>();

  const location = useLocation();
  const { data, isLoading, error } = useKafkaCluster(kafkaId, {
    fields: 'name,namespace,status,kafkaVersion,creationTimestamp,listeners,conditions',
  });

  // Fetch all clusters for the cluster switcher
  const { data: clustersData } = useKafkaClusters({ 
    fields: 'name,namespace', 
    page: { size: 1000 },
  });

  // Fetch topic data if we're on a topic detail page
  const { data: topicData } = useTopic(
    kafkaId,
    topicId,
    { fields: ['name'] }
  );
  
  const { data: connectorData } = useConnector(
    connectorId,
  );

  const { data: connectClusterData } = useConnectCluster(
    connectClusterId,
  );

  // Fetch user data if we're on a user detail page
  const { data: userData } = useUser(
    kafkaId,
    userId,
    { fields: ['username'] }
  );

  if (isLoading) {
    return (
      <Page masthead={<AppMasthead showSidebarToggle={false} />}>
        <PageSection>
          <EmptyState>
            <Spinner size="xl" />
            <Title headingLevel="h1" size="lg">
              {t('common.loading')}
            </Title>
          </EmptyState>
        </PageSection>
      </Page>
    );
  }

  if (error) {
    return (
      <Page masthead={<AppMasthead showSidebarToggle={false} />}>
        <PageSection>
          <EmptyState>
            <Title headingLevel="h1" size="lg">
              {t('common.error')}
            </Title>
            <EmptyStateBody>{error.message}</EmptyStateBody>
          </EmptyState>
        </PageSection>
      </Page>
    );
  }

  const cluster = data?.data;
  const clusterName = cluster?.attributes.name || kafkaId || '';

  // Determine current page from location
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const currentPage = pathSegments[pathSegments.length - 1];
  
  // Check if we're on a topic detail page
  const isTopicDetailPage = !!topicId;
  const topicName = topicData?.data?.attributes?.name || topicId || '';
  
  // Check if we're on a nodes page (overview or rebalances tab)
  const isNodesPage = pathSegments.includes('nodes') && !nodeId;
  const nodesTab = isNodesPage ? currentPage : null;
  
  // Check if we're on a node detail page
  const isNodeDetailPage = !!nodeId;
  const nodeTab = isNodeDetailPage ? currentPage : null;
  
  // Check if we're on a connect page (connectors or clusters tab)
  const isConnectorDetailPage = !!connectorId;
  const connectorName = connectorData?.data?.attributes?.name || connectorId || '';
  const isConnectClusterDetailPage = !!connectClusterId;
  const connectClusterName = connectClusterData?.data?.attributes?.name || connectClusterId || '';
  const isConnectPage = pathSegments.includes('connect') && !isConnectorDetailPage && !isConnectClusterDetailPage;
  const connectTab = isConnectPage ? currentPage : null;
  
  // Check if we're on a group detail page
  const isGroupDetailPage = !!groupId;
  const groupTab = isGroupDetailPage ? currentPage : null;
  
  // Check if we're on a user detail page
  const isUserDetailPage = !!userId;
  const username = userData?.data?.attributes?.username || userId || '';
  
  // Map path segments to readable names
  const getPageTitle = (segment: string): string => {
    const pageMap: Record<string, string> = {
      overview: t('kafka.overview'),
      topics: t('kafka.topics'),
      nodes: t('kafka.nodes'),
      connect: t('kafka.connect.title'),
      users: t('kafka.users'),
      groups: t('groups.title'),
    };
    return pageMap[segment] || segment;
  };
  
  // Get nodes tab title
  const getNodesTabTitle = (tab: string): string => {
    const tabMap: Record<string, string> = {
      overview: t('nodes.tabs.overview'),
      rebalances: t('nodes.tabs.rebalances'),
    };
    return tabMap[tab] || tab;
  };
  
  // Get node detail tab title
  const getNodeTabTitle = (tab: string): string => {
    const tabMap: Record<string, string> = {
      configuration: t('topics.tabs.configuration'),
    };
    return tabMap[tab] || tab;
  };
  
  // Get connect tab title
  const getConnectTabTitle = (tab: string): string => {
    const tabMap: Record<string, string> = {
      connectors: t('kafka.connect.connectors'),
      clusters: t('kafka.connect.connectClusters'),
    };
    return tabMap[tab] || tab;
  };
  
  // Get group tab title
  const getGroupTabTitle = (tab: string): string => {
    const tabMap: Record<string, string> = {
      members: t('groups.members'),
      configuration: t('topics.tabs.configuration'),
    };
    return tabMap[tab] || tab;
  };

  const breadcrumb = (
    <Breadcrumb>
      <BreadcrumbItem>
        <Link to="/">
          <HomeIcon />
        </Link>
      </BreadcrumbItem>
      <BreadcrumbItem>
        <Link to={`/kafka/${kafkaId}`}>
          {clusterName}
        </Link>
      </BreadcrumbItem>
      {isTopicDetailPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/topics`}>
            {t('kafka.topics')}
          </Link>
        </BreadcrumbItem>
      )}
      {isTopicDetailPage && (
        <BreadcrumbItem isActive>
          {topicName}
        </BreadcrumbItem>
      )}
      {isNodesPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/nodes`}>
            {t('kafka.nodes')}
          </Link>
        </BreadcrumbItem>
      )}
      {isNodesPage && nodesTab && nodesTab !== 'nodes' && (
        <BreadcrumbItem isActive>
          {getNodesTabTitle(nodesTab)}
        </BreadcrumbItem>
      )}
      {isNodeDetailPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/nodes`}>
            {t('kafka.nodes')}
          </Link>
        </BreadcrumbItem>
      )}
      {isNodeDetailPage && (
        <BreadcrumbItem>
          {t('nodes.brokerTitle', { nodeId })}
        </BreadcrumbItem>
      )}
      {isNodeDetailPage && nodeTab && nodeTab !== nodeId && (
        <BreadcrumbItem isActive>
          {getNodeTabTitle(nodeTab)}
        </BreadcrumbItem>
      )}
      {(isConnectPage || isConnectorDetailPage || isConnectClusterDetailPage) && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/connect`}>
            {t('kafka.connect.title')}
          </Link>
        </BreadcrumbItem>
      )}
      {isConnectPage && connectTab && connectTab !== 'connect' && (
        <BreadcrumbItem isActive>
          {getConnectTabTitle(connectTab)}
        </BreadcrumbItem>
      )}
      {isConnectorDetailPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/connect/connectors`}>
            {t('kafka.connect.connectors')}
          </Link>
        </BreadcrumbItem>
      )}
      {isConnectorDetailPage && (
        <BreadcrumbItem isActive>
          {connectorName}
        </BreadcrumbItem>
      )}
      {isConnectClusterDetailPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/connect/clusters`}>
            {t('kafka.connect.connectClusters')}
          </Link>
        </BreadcrumbItem>
      )}
      {isConnectClusterDetailPage && (
        <BreadcrumbItem isActive>
          {connectClusterName}
        </BreadcrumbItem>
      )}
      {isGroupDetailPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/groups`}>
            {t('groups.title')}
          </Link>
        </BreadcrumbItem>
      )}
      {isGroupDetailPage && groupTab && groupTab !== groupId && (
        <BreadcrumbItem isActive>
          {getGroupTabTitle(groupTab)}
        </BreadcrumbItem>
      )}
      {isUserDetailPage && (
        <BreadcrumbItem>
          <Link to={`/kafka/${kafkaId}/users`}>
            {t('kafka.users')}
          </Link>
        </BreadcrumbItem>
      )}
      {isUserDetailPage && (
        <BreadcrumbItem isActive>
          {username}
        </BreadcrumbItem>
      )}
      {!isTopicDetailPage && !isNodesPage && !isNodeDetailPage && !isConnectPage && !isConnectorDetailPage && !isConnectClusterDetailPage && !isGroupDetailPage && !isUserDetailPage && currentPage !== kafkaId && (
        <BreadcrumbItem isActive>
          {getPageTitle(currentPage)}
        </BreadcrumbItem>
      )}
    </Breadcrumb>
  );

  const clusters = clustersData?.data || [];

  return (
    <Page
      masthead={
        <AppMasthead
          showSidebarToggle={true}
          clusters={clusters}
          currentClusterId={kafkaId}
        />
      }
      sidebar={<KafkaClusterSidebar />}
      breadcrumb={breadcrumb}
      banner={<ReconciliationControls cluster={cluster} />}
      isBreadcrumbWidthLimited
    >
      <Outlet />
    </Page>
  );
}