/**
 * Node Detail Page - Shows details for a specific Kafka node/broker
 */

import { useParams, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Tabs,
  Tab,
  TabTitleText,
  Title,
  EmptyState,
  EmptyStateBody,
  Spinner,
} from '@patternfly/react-core';
import { useNodeConfig } from '../api/hooks/useNodes';

export function NodeDetailPage() {
  const { t } = useTranslation();
  const { kafkaId, nodeId } = useParams<{ kafkaId: string; nodeId: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  // Fetch node configuration to verify node exists
  const { data, isLoading, error } = useNodeConfig(kafkaId, nodeId);

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const lastSegment = pathSegments[pathSegments.length - 1];
  
  // Default to 'configuration' if we're at the node root or if last segment is the nodeId
  const activeTab = lastSegment === nodeId ? 'configuration' : lastSegment;

  const handleTabSelect = (_event: React.MouseEvent, tabKey: string | number) => {
    navigate(`/kafka/${kafkaId}/nodes/${nodeId}/${tabKey}`);
  };

  if (isLoading) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {t('nodes.brokerTitle', { nodeId })}
        </Title>
      </PageSection>
      <PageSection>
        <Tabs
          activeKey={activeTab}
          onSelect={handleTabSelect}
          aria-label={t('nodes.brokerTitle', { nodeId })}
          role="region"
        >
          <Tab
            eventKey="configuration"
            title={<TabTitleText>{t('topics.tabs.configuration')}</TabTitleText>}
            aria-label={t('topics.tabs.configuration')}
          />
        </Tabs>
      </PageSection>
      <Outlet context={{ nodeConfig: data?.data }} />
    </>
  );
}