/**
 * Connect Clusters Table Component
 *
 * Displays a table of Kafka Connect clusters with filtering and pagination
 */

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  SearchInput,
  Tooltip,
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
import { HelpIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useConnectClusters } from '@/api/hooks/useConnect';
import {
  LoadingEmptyState,
  ErrorEmptyState,
  NoDataEmptyState,
  NoResultsEmptyState,
} from '@/components/EmptyStates';
import { useTableState } from '@/hooks';

interface ConnectClustersTableProps {
  kafkaId: string | undefined;
}

type SortableColumn = 'name' | 'version';

export function ConnectClustersTable({ kafkaId }: ConnectClustersTableProps) {
  const { t } = useTranslation();
  const [searchValue, setSearchValue] = useState('');

  // Use table state hook for pagination and sorting
  const table = useTableState<SortableColumn>({
    initialSortColumn: 'name',
    initialSortDirection: 'asc',
  });

  const { data, isLoading, error } = useConnectClusters(kafkaId, {
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

  const connectClusters = data?.data || [];
  const totalItems = data?.meta?.page?.total || 0;
  const page = data?.meta?.page?.pageNumber;

  if (error) {
    return <ErrorEmptyState error={error} title="Error loading connect clusters" />;
  }

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <SearchInput
              placeholder={t('kafka.connect.searchClusters', 'Search connect clusters')}
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
        <LoadingEmptyState message="Loading connect clusters" />
      ) : connectClusters.length === 0 ? (
        searchValue ? (
          <NoResultsEmptyState
            message={t('kafka.connect.noClustersMatch', 'No connect clusters match your search criteria')}
          />
        ) : (
          <NoDataEmptyState
            entityName="connect clusters"
            message={t('kafka.connect.noClustersAvailable', 'No connect clusters are available')}
          />
        )
      ) : (
        <Table aria-label="Connect clusters table" variant="compact">
          <Thead>
            <Tr>
              <Th sort={table.getSortParams('name')}>{t('kafka.connect.name', 'Name')}</Th>
              <Th sort={table.getSortParams('version')}>{t('kafka.connect.version', 'Version')}</Th>
              <Th>
                {t('kafka.connect.workers', 'Workers')}{' '}
                <Tooltip content={t('kafka.connect.workersTooltip', 'Number of worker nodes')}>
                  <HelpIcon />
                </Tooltip>
              </Th>
            </Tr>
          </Thead>
          <Tbody>
            {connectClusters.map((cluster) => (
              <Tr key={cluster.id}>
                <Td dataLabel={t('kafka.connect.name', 'Name')}>
                  <Link
                    to={`/kafka/${kafkaId}/connect/clusters/${encodeURIComponent(cluster.id)}`}
                  >
                    {cluster.attributes.name}
                  </Link>
                </Td>
                <Td dataLabel={t('kafka.connect.version', 'Version')}>
                  {cluster.attributes.version || '-'}
                </Td>
                <Td dataLabel={t('kafka.connect.workers', 'Workers')}>
                  {cluster.attributes.replicas ?? '-'}
                </Td>
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