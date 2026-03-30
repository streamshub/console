/**
 * Connectors Table Component
 *
 * Displays a table of Kafka connectors with filtering and pagination
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  SearchInput,
  EmptyState,
  EmptyStateBody,
  Spinner,
  Pagination,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ThProps,
} from '@patternfly/react-table';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  HistoryIcon,
} from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useConnectors } from '../api/hooks/useConnect';
import { ConnectorState, ConnectorType, EnrichedConnector } from '../api/types';
import { ManagedConnectorLabel } from './ManagedConnectorLabel';
import stopIcon from '/stop-icon.svg';

interface ConnectorsTableProps {
  kafkaId: string | undefined;
}

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

export function ConnectorsTable({ kafkaId }: ConnectorsTableProps) {
  const { t } = useTranslation();
  const [searchValue, setSearchValue] = useState('');
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);

  const { data, isLoading, error } = useConnectors(kafkaId, {
    pageSize: perPage,
    name: searchValue || undefined,
    sort: sortBy,
    sortDir: sortDirection,
  });

  const connectors = data?.data || [];
  const totalItems = data?.meta?.page?.total || 0;

  const handlePageChange = (newPage: number, newPerPage: number) => {
    setPage(newPage);
    setPerPage(newPerPage);
  };

  const handleSort = (columnKey: string) => {
    if (sortBy === columnKey) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(columnKey);
      setSortDirection('asc');
    }
  };

  const getSortParams = (columnKey: string): ThProps['sort'] => ({
    sortBy: {
      index: sortBy === columnKey ? 0 : undefined,
      direction: sortDirection,
    },
    onSort: () => handleSort(columnKey),
    columnIndex: 0,
  });

  if (error) {
    return (
      <EmptyState>
        <ExclamationCircleIcon />
        <h4>Error loading connectors</h4>
        <EmptyStateBody>{error.message}</EmptyStateBody>
      </EmptyState>
    );
  }

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <SearchInput
              placeholder={t('kafka.connect.searchConnectors', 'Search connectors')}
              value={searchValue}
              onChange={(_event, value) => setSearchValue(value)}
              onClear={() => setSearchValue('')}
            />
          </ToolbarItem>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalItems}
              page={page}
              perPage={perPage}
              onSetPage={(_, newPage) => handlePageChange(newPage, perPage)}
              onPerPageSelect={(_, newPerPage) => handlePageChange(1, newPerPage)}
              variant="top"
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      {isLoading ? (
        <EmptyState>
          <Spinner />
          <h4>Loading connectors</h4>
        </EmptyState>
      ) : connectors.length === 0 ? (
        <EmptyState>
          <h4>{t('kafka.connect.noConnectors', 'No connectors found')}</h4>
          <EmptyStateBody>
            {searchValue
              ? t('kafka.connect.noConnectorsMatch', 'No connectors match your search criteria')
              : t('kafka.connect.noConnectorsAvailable', 'No connectors are available')}
          </EmptyStateBody>
        </EmptyState>
      ) : (
        <Table aria-label="Connectors table" variant="compact">
          <Thead>
            <Tr>
              <Th {...getSortParams('name')}>{t('kafka.connect.name', 'Name')}</Th>
              <Th {...getSortParams('connect-cluster')}>
                {t('kafka.connect.connectCluster', 'Connect cluster')}
              </Th>
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
                  {connector.meta?.managed === true && <ManagedConnectorLabel />}
                </Td>
                <Td dataLabel={t('kafka.connect.connectCluster', 'Connect cluster')}>
                  {connector.connectClusterId ? (
                    <Link
                      to={`/kafka/${kafkaId}/connect/clusters/${connector.connectClusterId}`}
                    >
                      {connector.connectClusterName}
                    </Link>
                  ) : (
                    '-'
                  )}
                </Td>
                <Td dataLabel={t('kafka.connect.type', 'Type')}>
                  {TypeLabel[connector.attributes.type]}
                </Td>
                <Td dataLabel={t('kafka.connect.state', 'State')}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {StateIcon[connector.attributes.state]}
                    {StateLabel[connector.attributes.state]}
                  </span>
                </Td>
                <Td dataLabel={t('kafka.connect.tasks', 'Tasks')}>{connector.replicas ?? '-'}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}

      <Toolbar>
        <ToolbarContent>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalItems}
              page={page}
              perPage={perPage}
              onSetPage={(_, newPage) => handlePageChange(newPage, perPage)}
              onPerPageSelect={(_, newPerPage) => handlePageChange(1, newPerPage)}
              variant="bottom"
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    </>
  );
}