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
  EmptyState,
  EmptyStateBody,
  Spinner,
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
  CheckCircleIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  HistoryIcon,
} from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useConnector } from '../api/hooks/useConnect';
import { ConnectorState, ConnectorType } from '../api/types';
import { ManagedConnectorLabel } from '../components/ManagedConnectorLabel';
import stopIcon from '/stop-icon.svg';

const StateIcon: Record<ConnectorState, React.ReactNode> = {
  UNASSIGNED: <PendingIcon />,
  RUNNING: <CheckCircleIcon color="var(--pf-t--global--icon--color--status--success--default)" />,
  PAUSED: <PauseCircleIcon />,
  STOPPED: <img src={stopIcon} alt="stopped" style={{ width: '1em', height: '1em' }} />,
  FAILED: <ExclamationCircleIcon color="var(--pf-t--global--icon--color--status--danger--default)" />,
  RESTARTING: <HistoryIcon />,
};

const StateLabel: Record<ConnectorState, string> = {
  UNASSIGNED: 'Unassigned',
  RUNNING: 'Running',
  PAUSED: 'Paused',
  STOPPED: 'Stopped',
  FAILED: 'Failed',
  RESTARTING: 'Restarting',
};

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
        <EmptyState>
          <Spinner />
          <h4>Loading connector details</h4>
        </EmptyState>
      </PageSection>
    );
  }

  if (error || !data) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <ExclamationCircleIcon />
          <h4>Error loading connector</h4>
          <EmptyStateBody>{error?.message || 'Connector not found'}</EmptyStateBody>
        </EmptyState>
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
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                {StateIcon[connector.attributes.state]}
                {StateLabel[connector.attributes.state]}
              </span>
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
              <EmptyState>
                <h4>{t('kafka.connect.noTasks', 'No tasks found')}</h4>
              </EmptyState>
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
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            {StateIcon[task.attributes.state]}
                            {StateLabel[task.attributes.state]}
                          </span>
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
              <EmptyState>
                <h4>{t('kafka.connect.noConfig', 'No configuration found')}</h4>
              </EmptyState>
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