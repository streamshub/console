/**
 * Topic Detail Page - Shows detailed information about a topic with tabs
 */

import { useParams, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useEffect } from 'react';
import {
  PageSection,
  Tabs,
  Tab,
  TabTitleText,
  EmptyState,
  EmptyStateBody,
  Spinner,
  Title,
} from '@patternfly/react-core';
import { useTopic } from '../api/hooks/useTopics';
import { useViewedTopics } from '../api/hooks/useViewedTopics';
import { ManagedTopicLabel } from '../components/ManagedTopicLabel';

export function TopicDetailPage() {
  const { t } = useTranslation();
  const { kafkaId, topicId } = useParams<{ kafkaId: string; topicId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { data, isLoading, error } = useTopic(kafkaId, topicId);
  const { addViewedTopic } = useViewedTopics(kafkaId);

  // Track this topic as viewed when data is loaded
  useEffect(() => {
    if (data?.data && kafkaId && topicId) {
      const topicName = data.data.attributes.name;
      if (topicName) {
        addViewedTopic(topicId, topicName);
      }
    }
  }, [data, kafkaId, topicId, addViewedTopic]);

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const currentTab = pathSegments[pathSegments.length - 1];
  
  // Default to 'messages' if we're at the topic root
  const activeTab = currentTab === topicId ? 'messages' : currentTab;

  const handleTabSelect = (_event: React.MouseEvent, tabKey: string | number) => {
    navigate(`/kafka/${kafkaId}/topics/${topicId}/${tabKey}`);
  };

  if (isLoading) {
    return (
      <PageSection>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h1" size="lg">
            {t('topics.details.loadingTopic')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection>
        <EmptyState>
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  const topic = data?.data;
  const topicName = topic?.attributes.name || topicId || '';

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {topicName}
          {topic?.meta?.managed === true && <ManagedTopicLabel />}
        </Title>
      </PageSection>
      <PageSection>
        <Tabs
          activeKey={activeTab}
          onSelect={handleTabSelect}
          aria-label={t('topics.details.title')}
          role="region"
        >
          <Tab
            eventKey="messages"
            title={<TabTitleText>{t('topics.tabs.messages')}</TabTitleText>}
            aria-label={t('topics.tabs.messages')}
          />
          <Tab
            eventKey="partitions"
            title={<TabTitleText>{t('topics.tabs.partitions')}</TabTitleText>}
            aria-label={t('topics.tabs.partitions')}
          />
          <Tab
            eventKey="groups"
            title={<TabTitleText>{t('topics.tabs.groups')}</TabTitleText>}
            aria-label={t('topics.tabs.groups')}
          />
          <Tab
            eventKey="configuration"
            title={<TabTitleText>{t('topics.tabs.configuration')}</TabTitleText>}
            aria-label={t('topics.tabs.configuration')}
          />
        </Tabs>
      </PageSection>
      <Outlet />
    </>
  );
}