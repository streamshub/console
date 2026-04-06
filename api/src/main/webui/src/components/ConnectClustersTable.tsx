/**
 * Connect Clusters Table Component
 *
 * Displays a table of Kafka Connect clusters with filtering and pagination
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
  Tooltip,
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
import { ExclamationCircleIcon, HelpIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useConnectClusters } from '../api/hooks/useConnect';

interface ConnectClustersTableProps {
  kafkaId: string | undefined;
}

export function ConnectClustersTable({ kafkaId }: ConnectClustersTableProps) {
  const { t } = useTranslation();
  const [searchValue, setSearchValue] = useState('');
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);

  const { data, isLoading, error } = useConnectClusters(kafkaId, {
    pageSize: perPage,
    name: searchValue || undefined,
    sort: sortBy,
    sortDir: sortDirection,
  });

  const connectClusters = data?.data || [];
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
        <h4>Error loading connect clusters</h4>
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
          <h4>Loading connect clusters</h4>
        </EmptyState>
      ) : connectClusters.length === 0 ? (
        <EmptyState>
          <h4>{t('kafka.connect.noClusters', 'No connect clusters found')}</h4>
          <EmptyStateBody>
            {searchValue
              ? t('kafka.connect.noClustersMatch', 'No connect clusters match your search criteria')
              : t('kafka.connect.noClustersAvailable', 'No connect clusters are available')}
          </EmptyStateBody>
        </EmptyState>
      ) : (
        <Table aria-label="Connect clusters table" variant="compact">
          <Thead>
            <Tr>
              <Th sort={getSortParams('name')}>{t('kafka.connect.name', 'Name')}</Th>
              <Th sort={getSortParams('version')}>{t('kafka.connect.version', 'Version')}</Th>
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