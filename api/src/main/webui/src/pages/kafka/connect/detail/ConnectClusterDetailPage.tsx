/**
 * Connect Cluster Detail Page
 *
 * Displays detailed information about a specific Kafka Connect cluster
 */

import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  PageSection,
  Tabs,
  Tab,
  TabTitleText,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  EmptyState,
  Tooltip,
  Label,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from '@patternfly/react-table';
import {
  HelpIcon,
} from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useConnectCluster } from '@/api/hooks/useConnect';
import { ConnectorType } from '@/api/types';
import { StatusLabel } from '@/components/StatusLabel';
import { CONNECTOR_STATE_CONFIG } from '@/components/StatusLabel/configs';
import { LoadingEmptyState, ErrorEmptyState } from '@/components/EmptyStates';


const TypeLabel: Record<ConnectorType, string> = {
  source: 'Source',
  sink: 'Sink',
  'source:mm': 'Mirror Source',
  'source:mm-checkpoint': 'Mirror Checkpoint',
  'source:mm-heartbeat': 'Mirror Heartbeat',
};

export function ConnectClusterDetailPage() {
  const { t } = useTranslation();
  const { kafkaId, connectClusterId } = useParams<{ kafkaId: string; connectClusterId: string }>();
  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);

  const { data, isLoading, error } = useConnectCluster(connectClusterId);

  const handleTabClick = (
    _event: React.MouseEvent<HTMLElement, MouseEvent>,
    tabIndex: string | number
  ) => {
    setActiveTabKey(tabIndex);
  };

  if (isLoading) {
    return (
      <PageSection isFilled>
        <LoadingEmptyState />
      </PageSection>
    );
  }

  if (error || !data) {
    return (
      <PageSection isFilled>
        <ErrorEmptyState
          title={t('kafka.connect.errorTitle', 'Error loading connect cluster')}
          message={error?.message || t('kafka.connect.errorMessage', 'Connect cluster not found')}
          error={error}
        />
      </PageSection>
    );
  }

  const cluster = data.data;
  const connectors = data.included || [];
  const plugins = cluster.attributes.plugins || [];

  return (
    <PageSection isFilled>
      <div style={{ marginBottom: '2rem' }}>
        <DescriptionList isHorizontal columnModifier={{ default: '2Col' }}>
          <DescriptionListGroup>
            <DescriptionListTerm>{t('kafka.connect.version', 'Version')}</DescriptionListTerm>
            <DescriptionListDescription>
              {cluster.attributes.version || '-'}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t('kafka.connect.workers', 'Workers')}{' '}
              <Tooltip content={t('kafka.connect.workersTooltip', 'Number of worker nodes')}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {cluster.attributes.replicas ?? '-'}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </div>

      <Tabs
        activeKey={activeTabKey}
        onSelect={handleTabClick}
        aria-label="Connect cluster details tabs"
        role="region"
      >
        <Tab
          eventKey={0}
          title={<TabTitleText>{t('kafka.connect.connectors', 'Connectors')}</TabTitleText>}
          aria-label="Connectors"
        >
          <div style={{ marginTop: '1rem' }}>
            {connectors.length === 0 ? (
              <EmptyState>
                <h4>{t('kafka.connect.noConnectors', 'No connectors found')}</h4>
              </EmptyState>
            ) : (
              <Table aria-label="Connectors" variant="compact">
                <Thead>
                  <Tr>
                    <Th>{t('kafka.connect.name', 'Name')}</Th>
                    <Th>{t('kafka.connect.type', 'Type')}</Th>
                    <Th>{t('kafka.connect.state', 'State')}</Th>
                    <Th>{t('kafka.connect.tasks', 'Tasks')}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {connectors.map((connector) => (
                    <Tr key={connector.id}>
                      <Td dataLabel={t('kafka.connect.name', 'Name')}>
                        <Link to={`/kafka/${kafkaId}/connect/connectors/${encodeURIComponent(connector.id)}`}>
                          {connector.attributes.name}
                        </Link>
                        {connector.meta?.managed && (
                          <Label color="blue" isCompact style={{ marginLeft: '8px' }}>
                            {t('kafka.connect.managed', 'Managed')}
                          </Label>
                        )}
                      </Td>
                      <Td dataLabel={t('kafka.connect.type', 'Type')}>
                        {TypeLabel[connector.attributes.type]}
                      </Td>
                      <Td dataLabel={t('kafka.connect.state', 'State')}>
                        <StatusLabel status={connector.attributes.state} config={CONNECTOR_STATE_CONFIG} />
                      </Td>
                      <Td dataLabel={t('kafka.connect.tasks', 'Tasks')}>
                        {cluster.attributes.replicas ?? '-'}
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </Tab>
        <Tab
          eventKey={1}
          title={<TabTitleText>{t('kafka.connect.plugins', 'Plugins')}</TabTitleText>}
          aria-label="Plugins"
        >
          <div style={{ marginTop: '1rem' }}>
            {plugins.length === 0 ? (
              <EmptyState>
                <h4>{t('kafka.connect.noPlugins', 'No plugins found')}</h4>
              </EmptyState>
            ) : (
              <Table aria-label="Plugins" variant="compact">
                <Thead>
                  <Tr>
                    <Th>{t('kafka.connect.class', 'Class')}</Th>
                    <Th>{t('kafka.connect.type', 'Type')}</Th>
                    <Th>{t('kafka.connect.version', 'Version')}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {plugins.map((plugin, index) => (
                    <Tr key={index}>
                      <Td dataLabel={t('kafka.connect.class', 'Class')}>{plugin.class}</Td>
                      <Td dataLabel={t('kafka.connect.type', 'Type')}>
                        {TypeLabel[plugin.type]}
                      </Td>
                      <Td dataLabel={t('kafka.connect.version', 'Version')}>{plugin.version}</Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </Tab>
      </Tabs>
    </PageSection>
  );
}