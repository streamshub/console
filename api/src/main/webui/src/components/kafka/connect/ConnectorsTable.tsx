/**
 * Connectors Table Component
 *
 * Displays a table of Kafka connectors with filtering and pagination
 */

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  SearchInput,
  Pagination,
  PaginationVariant,
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
import { useConnectors } from '@/api/hooks/useConnect';
import { ConnectorType } from '@/api/types';
import { ManagedConnectorLabel } from './ManagedConnectorLabel';
import { StatusLabel } from '@/components/StatusLabel';
import { CONNECTOR_STATE_CONFIG } from '@/components/StatusLabel/configs';
import {
  LoadingEmptyState,
  ErrorEmptyState,
  NoDataEmptyState,
  NoResultsEmptyState,
} from '@/components/EmptyStates';
import { useTableState } from '@/hooks';

interface ConnectorsTableProps {
  kafkaId: string | undefined;
}

type SortableColumn = 'name' | 'connect-cluster';

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

  // Use table state hook for pagination and sorting
  const table = useTableState<SortableColumn>({
    initialSortColumn: 'name',
    initialSortDirection: 'asc',
  });

  const { data, isLoading, error } = useConnectors(kafkaId, {
    pageSize: table.pageSize,
    pageCursor: table.pageCursor,
    name: searchValue || undefined,
    sort: table.sortBy,
    sortDir: table.sortDirection,
  });

  // Update table state with API response data
  useEffect(() => {
    table.setData(data);
  }, [data, table]);

  // Reset pagination when search changes
  useEffect(() => {
    table.resetPagination();
  }, [searchValue, table]);

  const connectors = data?.data || [];
  const totalItems = data?.meta?.page?.total || 0;
  const page = data?.meta?.page?.pageNumber;

  if (error) {
    return <ErrorEmptyState error={error} title="Error loading connectors" />;
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
              perPage={table.pageSize}
              onPerPageSelect={table.handlePerPageChange}
              onNextClick={table.handleNextPage}
              onPreviousClick={table.handlePrevPage}
              variant={PaginationVariant.top}
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      {isLoading ? (
        <LoadingEmptyState message="Loading connectors" />
      ) : connectors.length === 0 ? (
        searchValue ? (
          <NoResultsEmptyState
            message={t('kafka.connect.noConnectorsMatch', 'No connectors match your search criteria')}
          />
        ) : (
          <NoDataEmptyState
            entityName="connectors"
            message={t('kafka.connect.noConnectorsAvailable', 'No connectors are available')}
          />
        )
      ) : (
        <Table aria-label="Connectors table" variant="compact">
          <Thead>
            <Tr>
              <Th sort={table.getSortParams('name')}>{t('kafka.connect.name', 'Name')}</Th>
              <Th sort={table.getSortParams('connect-cluster')}>
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
                  <StatusLabel status={connector.attributes.state} config={CONNECTOR_STATE_CONFIG} />
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
              perPage={table.pageSize}
              onPerPageSelect={table.handlePerPageChange}
              onNextClick={table.handleNextPage}
              onPreviousClick={table.handlePrevPage}
              variant={PaginationVariant.bottom}
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    </>
  );
}