import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  Button,
  Truncate,
  Pagination,
  PaginationVariant,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  SearchInput,
} from '@patternfly/react-core';
import { KafkaCluster } from '@/api/types';
import { NoDataEmptyState, NoResultsEmptyState } from '@/components/EmptyStates';

export function ClustersTable({
  clusters,
  totalCount,
  page,
  perPage,
  onPageChange,
  onPerPageChange,
  sortBy,
  sortDirection,
  onSort,
  filterName,
  onFilterNameChange,
}: {
  clusters: KafkaCluster[] | undefined;
  totalCount?: number;
  page: number;
  perPage: number;
  onPageChange: (page: number) => void;
  onPerPageChange: (perPage: number) => void;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
  onSort?: (column: string, direction: 'asc' | 'desc') => void;
  filterName?: string;
  onFilterNameChange?: (name: string) => void;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState(filterName || '');

  const getSortParams = (columnName: string): ThProps['sort'] | undefined => {
    if (!onSort) return undefined;
    
    return {
      sortBy: {
        index: sortBy === columnName ? 0 : undefined,
        direction: sortDirection || 'asc',
      },
      onSort: (_event, _index, direction) => {
        onSort(columnName, direction);
      },
      columnIndex: 0,
    };
  };

  const handleSearchSubmit = () => {
    if (onFilterNameChange) {
      onFilterNameChange(searchValue);
    }
  };

  const handleSearchClear = () => {
    setSearchValue('');
    if (onFilterNameChange) {
      onFilterNameChange('');
    }
  };

  if (!clusters || clusters.length === 0) {
    // Show "no results" if a filter is active, otherwise "no data"
    const hasActiveFilter = filterName && filterName.trim().length > 0;
    
    if (hasActiveFilter) {
      return (
        <NoResultsEmptyState
          title={t('kafka.noResultsTitle', 'No clusters found')}
          message={t('kafka.noResultsMessage', 'No clusters match your search criteria. Try adjusting your filters.')}
        />
      );
    }
    
    return (
      <NoDataEmptyState
        title={t('kafka.noCluster', 'No Kafka clusters')}
        message={t('kafka.noClusterDescription', 'No Kafka clusters are currently available.')}
      />
    );
  }

  return (
    <>
      {onFilterNameChange && (
        <Toolbar>
          <ToolbarContent>
            <ToolbarItem>
              <SearchInput
                placeholder={t('kafka.filterByName', 'Filter by name')}
                value={searchValue}
                onChange={(_event, value) => setSearchValue(value)}
                onSearch={handleSearchSubmit}
                onClear={handleSearchClear}
                aria-label={t('kafka.filterByName', 'Filter by name')}
              />
            </ToolbarItem>
            {totalCount !== undefined && totalCount > 0 && (
              <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                <Pagination
                  itemCount={totalCount}
                  page={page}
                  perPage={perPage}
                  onSetPage={(_event, page) => onPageChange(page)}
                  onPerPageSelect={(_event, perPage) => onPerPageChange(perPage)}
                  variant={PaginationVariant.top}
                  perPageOptions={[
                    { title: '10', value: 10 },
                    { title: '20', value: 20 },
                    { title: '50', value: 50 },
                    { title: '100', value: 100 },
                  ]}
                  isCompact
                />
              </ToolbarItem>
            )}
          </ToolbarContent>
        </Toolbar>
      )}
      <Table aria-label={t('kafka.clusterList', 'Kafka clusters')} variant="compact">
        <Thead>
          <Tr>
            <Th width={25} sort={getSortParams('name')}>{t('kafka.name', 'Name')}</Th>
            <Th>{t('kafka.namespace', 'Namespace')}</Th>
            <Th>{t('kafka.version', 'Kafka Version')}</Th>
            <Th>{t('kafka.status', 'Status')}</Th>
            <Th modifier="fitContent">{t('common.actions', 'Actions')}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {clusters.map((cluster) => (
            <Tr key={cluster.id}>
              <Td dataLabel={t('kafka.name', 'Name')}>
                <Truncate content={cluster.attributes.name} />
              </Td>
              <Td dataLabel={t('kafka.namespace', 'Namespace')}>
                {cluster.attributes.namespace || t('common.notAvailable', 'N/A')}
              </Td>
              <Td dataLabel={t('kafka.version', 'Kafka Version')}>
                {cluster.attributes.kafkaVersion || t('common.notAvailable', 'N/A')}
              </Td>
              <Td dataLabel={t('kafka.status', 'Status')}>
                {cluster.attributes.status || t('common.notAvailable', 'N/A')}
              </Td>
              <Td dataLabel={t('common.actions', 'Actions')} modifier="fitContent">
                <Button
                  variant="primary"
                  onClick={() => navigate(`/kafka/${cluster.id}`)}
                >
                  {t('common.view', 'View')}
                </Button>
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
      {totalCount !== undefined && totalCount > 0 && (
        <Pagination
          itemCount={totalCount}
          page={page}
          perPage={perPage}
          onSetPage={(_event, page) => onPageChange(page)}
          onPerPageSelect={(_event, perPage) => onPerPageChange(perPage)}
          variant={PaginationVariant.bottom}
          perPageOptions={[
            { title: '10', value: 10 },
            { title: '20', value: 20 },
            { title: '50', value: 50 },
            { title: '100', value: 100 },
          ]}
          isCompact
        />
      )}
    </>
  );
}