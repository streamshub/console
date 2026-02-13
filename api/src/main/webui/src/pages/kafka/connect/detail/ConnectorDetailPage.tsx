/**
 * Connector Detail Page
 *
 * Displays detailed information about a specific Kafka connector
 */

import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  PageSection,
  Title,
  Tabs,
  Tab,
  TabTitleText,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from '@patternfly/react-table';
import { useTranslation } from 'react-i18next';
import { useConnector } from '@/api/hooks/useConnect';
import { ConnectorType } from '@/api/types';
import { ManagedConnectorLabel } from '@/components/kafka/connect/ManagedConnectorLabel';
import { StatusLabel } from '@/components/StatusLabel';
import { CONNECTOR_STATE_CONFIG } from '@/components/StatusLabel/configs';
import {
  LoadingEmptyState,
  ErrorEmptyState,
  NoDataEmptyState,
} from '@/components/EmptyStates';


const TypeLabel: Record<ConnectorType, string> = {
  source: 'Source',
  sink: 'Sink',
  'source:mm': 'Mirror Source',
  'source:mm-checkpoint': 'Mirror Checkpoint',
  'source:mm-heartbeat': 'Mirror Heartbeat',
};

export function ConnectorDetailPage() {
  const { t } = useTranslation();
  const { connectorId } = useParams<{ connectorId: string }>();
  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);

  const { data, isLoading, error } = useConnector(connectorId);

  const handleTabClick = (
    _event: React.MouseEvent<HTMLElement, MouseEvent>,
    tabIndex: string | number
  ) => {
    setActiveTabKey(tabIndex);
  };

  if (isLoading) {
    return (
      <PageSection isFilled>
        <LoadingEmptyState message="Loading connector details" />
      </PageSection>
    );
  }

  if (error || !data) {
    return (
      <PageSection isFilled>
        <ErrorEmptyState
          error={error}
          title="Error loading connector"
          message={error?.message || 'Connector not found'}
        />
      </PageSection>
    );
  }

  const connector = data.data;
  const tasks = data.included?.filter((item) => item.type === 'connectorTasks') || [];
  const config = connector.attributes.config || {};
  const configEntries = Object.entries(config);
  const connectorName = connector.attributes.name || connectorId || '';

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {connectorName}
          {connector.meta?.managed === true && <ManagedConnectorLabel />}
        </Title>
      </PageSection>
      <PageSection>
        <DescriptionList isHorizontal columnModifier={{ default: '2Col' }}>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t('kafka.connect.connectorWorkerId', 'Connector worker ID')}
            </DescriptionListTerm>
            <DescriptionListDescription>
              {connector.attributes.workerId || '-'}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t('kafka.connect.class', 'Class')}</DescriptionListTerm>
            <DescriptionListDescription>
              {config['connector.class'] || '-'}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t('kafka.connect.state', 'State')}</DescriptionListTerm>
            <DescriptionListDescription>
              <StatusLabel status={connector.attributes.state} config={CONNECTOR_STATE_CONFIG} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t('kafka.connect.type', 'Type')}</DescriptionListTerm>
            <DescriptionListDescription>
              {TypeLabel[connector.attributes.type]}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t('kafka.connect.topics', 'Topics')}</DescriptionListTerm>
            <DescriptionListDescription>
              {connector.attributes.topics && connector.attributes.topics.length > 0
                ? connector.attributes.topics.join(', ')
                : '-'}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t('kafka.connect.maxTasks', 'Max tasks')}</DescriptionListTerm>
            <DescriptionListDescription>
              {config['tasks.max'] || '-'}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </PageSection>
      <PageSection isFilled>
        <Tabs
          activeKey={activeTabKey}
          onSelect={handleTabClick}
          aria-label="Connector details tabs"
          role="region"
        >
        <Tab
          eventKey={0}
          title={<TabTitleText>{t('kafka.connect.tasks', 'Tasks')}</TabTitleText>}
          aria-label="Tasks"
        >
          <div style={{ marginTop: '1rem' }}>
            {tasks.length === 0 ? (
              <NoDataEmptyState
                entityName="tasks"
                message={t('kafka.connect.noTasks', 'No tasks found')}
              />
            ) : (
              <Table aria-label="Connector tasks" variant="compact">
                <Thead>
                  <Tr>
                    <Th>{t('kafka.connect.taskId', 'Task ID')}</Th>
                    <Th>{t('kafka.connect.state', 'State')}</Th>
                    <Th>{t('kafka.connect.workerId', 'Worker ID')}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {tasks.map((task) => {
                    if (task.type !== 'connectorTasks') return null;
                    return (
                      <Tr key={task.id}>
                        <Td dataLabel={t('kafka.connect.taskId', 'Task ID')}>
                          {task.attributes.taskId}
                        </Td>
                        <Td dataLabel={t('kafka.connect.state', 'State')}>
                          <StatusLabel status={task.attributes.state} config={CONNECTOR_STATE_CONFIG} />
                        </Td>
                        <Td dataLabel={t('kafka.connect.workerId', 'Worker ID')}>
                          {task.attributes.workerId}
                        </Td>
                      </Tr>
                    );
                  })}
                </Tbody>
              </Table>
            )}
          </div>
        </Tab>
        <Tab
          eventKey={1}
          title={<TabTitleText>{t('kafka.connect.configuration', 'Configuration')}</TabTitleText>}
          aria-label="Configuration"
        >
          <div style={{ marginTop: '1rem' }}>
            {configEntries.length === 0 ? (
              <NoDataEmptyState
                entityName="configuration"
                message={t('kafka.connect.noConfig', 'No configuration found')}
              />
            ) : (
              <Table aria-label="Connector configuration" variant="compact">
                <Thead>
                  <Tr>
                    <Th width={40}>{t('kafka.connect.property', 'Property')}</Th>
                    <Th>{t('kafka.connect.value', 'Value')}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {configEntries.map(([key, value]) => (
                    <Tr key={key}>
                      <Td dataLabel={t('kafka.connect.property', 'Property')}>{key}</Td>
                      <Td dataLabel={t('kafka.connect.value', 'Value')}>{value ?? '-'}</Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </Tab>
        </Tabs>
      </PageSection>
    </>
  );
}