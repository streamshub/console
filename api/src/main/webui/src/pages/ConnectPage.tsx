/**
 * Kafka Connect Page
 *
 * Main page for Kafka Connect with tabs for Connectors and Connect Clusters
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

export function ConnectPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const lastSegment = pathSegments[pathSegments.length - 1];
  
  // Default to 'connectors' if we're at the connect root or if last segment is 'connect'
  const activeTab = lastSegment === 'connect' || lastSegment === kafkaId ? 'connectors' : lastSegment;

  const handleTabSelect = (_event: React.MouseEvent, tabKey: string | number) => {
    navigate(`/kafka/${kafkaId}/connect/${tabKey}`);
  };

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {t('kafka.connect.title')}
        </Title>
      </PageSection>
      <PageSection>
        <Tabs
          activeKey={activeTab}
          onSelect={handleTabSelect}
          aria-label={t('kafka.connect.title')}
          role="region"
        >
          <Tab
            eventKey="connectors"
            title={<TabTitleText>{t('kafka.connect.connectors')}</TabTitleText>}
            aria-label={t('kafka.connect.connectors')}
          />
          <Tab
            eventKey="clusters"
            title={<TabTitleText>{t('kafka.connect.connectClusters')}</TabTitleText>}
            aria-label={t('kafka.connect.connectClusters')}
          />
        </Tabs>
      </PageSection>
      <Outlet />
    </>
  );
}