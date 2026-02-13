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
  Label,
  Split,
  SplitItem,
  Spinner,
  Tooltip,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
} from '@patternfly/react-icons';
import { useNodes } from '@/api/hooks/useNodes';

export function NodesPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  // Fetch nodes data for status labels
  const { data, isLoading } = useNodes(kafkaId, { pageSize: 1 });
  const summary = data?.meta?.summary;
  const totalNodes = data?.meta?.page?.total || 0;

  // Calculate healthy and unhealthy nodes
  const healthyNodes = summary?.statuses?.combined?.Healthy ?? 0;
  const unhealthyNodes = summary?.statuses?.combined?.Unhealthy ?? 0;

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
          <Split hasGutter style={{ alignItems: 'center', display: 'flex' }}>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>{t('nodes.title')}</SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Label icon={isLoading ? <Spinner size="sm" /> : undefined}>
                {totalNodes}&nbsp;total
              </Label>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('nodes.statusLabels.healthyTooltip', 'Number of healthy nodes')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <CheckCircleIcon />}
                  color="green"
                >
                  {healthyNodes}
                </Label>
              </Tooltip>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('nodes.statusLabels.unhealthyTooltip', 'Number of unhealthy nodes')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <ExclamationTriangleIcon />}
                  color="orange"
                >
                  {unhealthyNodes}
                </Label>
              </Tooltip>
            </SplitItem>
          </Split>
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