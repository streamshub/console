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
import { useKafkaCluster, useKafkaClusters } from '../api/hooks/useKafkaClusters';
import { useTopic } from '../api/hooks/useTopics';
import { KafkaClusterSidebar } from '../components/KafkaClusterSidebar';
import { AppMasthead } from '../components/AppMasthead';

export function KafkaLayout() {
  const { t } = useTranslation();
  const { kafkaId, topicId } = useParams<{ kafkaId: string; topicId?: string }>();
  const location = useLocation();
  const { data, isLoading, error } = useKafkaCluster(kafkaId);
  
  // Fetch all clusters for the cluster switcher
  const { data: clustersData } = useKafkaClusters({ pageSize: 1000 });
  
  // Fetch topic data if we're on a topic detail page
  const { data: topicData } = useTopic(
    kafkaId,
    topicId,
    { fields: ['name'] }
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
  
  // Map path segments to readable names
  const getPageTitle = (segment: string): string => {
    const pageMap: Record<string, string> = {
      overview: t('kafka.overview'),
      topics: t('kafka.topics'),
      nodes: t('kafka.nodes'),
      connect: t('kafka.connect'),
      users: t('kafka.users'),
      groups: t('groups.title'),
    };
    return pageMap[segment] || segment;
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
      {!isTopicDetailPage && currentPage !== kafkaId && (
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
      isBreadcrumbWidthLimited
    >
      <Outlet />
    </Page>
  );
}