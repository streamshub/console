/**
 * Nodes Page - Shows Kafka nodes with Overview and Rebalance tabs
 */

import { useParams, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Tabs,
  Tab,
  TabTitleText,
  Title,
} from '@patternfly/react-core';

export function NodesPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const lastSegment = pathSegments[pathSegments.length - 1];
  
  // Default to 'overview' if we're at the nodes root or if last segment is 'nodes'
  const activeTab = lastSegment === 'nodes' || lastSegment === kafkaId ? 'overview' : lastSegment;

  const handleTabSelect = (_event: React.MouseEvent, tabKey: string | number) => {
    navigate(`/kafka/${kafkaId}/nodes/${tabKey}`);
  };

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {t('nodes.title')}
        </Title>
      </PageSection>
      <PageSection>
        <Tabs
          activeKey={activeTab}
          onSelect={handleTabSelect}
          aria-label={t('nodes.title')}
          role="region"
        >
          <Tab
            eventKey="overview"
            title={<TabTitleText>{t('nodes.tabs.overview')}</TabTitleText>}
            aria-label={t('nodes.tabs.overview')}
          />
          <Tab
            eventKey="rebalances"
            title={<TabTitleText>{t('nodes.tabs.rebalances')}</TabTitleText>}
            aria-label={t('nodes.tabs.rebalances')}
          />
        </Tabs>
      </PageSection>
      <Outlet />
    </>
  );
}