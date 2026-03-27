/**
 * Topics Page - List all topics in a Kafka cluster
 */

import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  EmptyState,
  EmptyStateBody,
  Spinner,
  Pagination,
  PaginationVariant,
  Switch,
  Tooltip,
  Icon,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
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
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ExclamationCircleIcon,
  CubesIcon,
} from '@patternfly/react-icons';
import { useTopics } from '../api/hooks/useTopics';
import { Topic } from '../api/types';
import { formatBytes } from '../utils/format';
import {
  TopicsFilterToolbar,
  TopicsFilterChips,
  TopicsFilterState,
  TopicsFilterType,
  TopicStatus,
} from '../components/TopicsFilter';
import { ManagedTopicLabel } from '../components/ManagedTopicLabel';

type SortableColumn = 'name' | 'totalLeaderLogBytes';

export function TopicsPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  
  const [pageSize, setPageSize] = useState(20);
  const [pageCursor, setPageCursor] = useState<string | undefined>(undefined);
  const [sortBy, setSortBy] = useState<SortableColumn>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [includeHidden, setIncludeHidden] = useState(false);

  // Advanced filtering state
  const [selectedFilterType, setSelectedFilterType] = useState<TopicsFilterType>('name');
  const [filterValue, setFilterValue] = useState('');
  const [filters, setFilters] = useState<TopicsFilterState>({});

  const { data, isLoading, error } = useTopics(kafkaId, {
    pageSize,
    pageCursor,
    sort: sortBy,
    sortDir: sortDirection,
    name: filters.name,
    id: filters.id,
    status: filters.status,
    includeHidden,
    fields: ['name', 'status', 'numPartitions', 'totalLeaderLogBytes', 'groups'],
  });

  const topics = data?.data || [];
  const totalItems = data?.meta.page.total || 0;
  const currentPage = data?.meta.page.pageNumber || 1;

  const handleSort = (column: SortableColumn) => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
    // Reset to first page when sorting changes
    setPageCursor(undefined);
  };

  const getSortParams = (column: SortableColumn): ThProps['sort'] => ({
    sortBy: {
      index: column === 'name' ? 0 : 3,
      direction: sortDirection,
      defaultDirection: 'asc',
    },
    onSort: () => handleSort(column),
    columnIndex: column === 'name' ? 0 : 3,
  });

  const getStatusLabel = (status: Topic['attributes']['status']) => {
    switch (status) {
      case 'FullyReplicated':
        return (
          <>
            <Icon status="success">
              <CheckCircleIcon />
            </Icon>
            &nbsp; {t('topics.status.FullyReplicated')}
          </>
        );
      case 'UnderReplicated':
        return (
          <>
            <Icon status="warning">
              <ExclamationTriangleIcon />
            </Icon>
            &nbsp;{t('topics.status.UnderReplicated')}
          </>
        );
      case 'PartiallyOffline':
        return (
          <>
            <Icon status="warning">
              <ExclamationTriangleIcon />
            </Icon>
            &nbsp;{t('topics.status.PartiallyOffline')}
          </>
        );
      case 'Unknown':
        return (
          <>
            <Icon status="warning">
              <ExclamationTriangleIcon />
            </Icon>
            &nbsp;{t('topics.status.Unknown')}
          </>
        );
      case 'Offline':
        return (
          <>
            <Icon status="danger">
              <ExclamationCircleIcon />
            </Icon>
            &nbsp;{t('topics.status.Offline')}
          </>
        );
      default:
        return null;
    }
  };

  const handleFilterSubmit = () => {
    if (selectedFilterType === 'status') {
      // Status filters are applied immediately via checkbox toggles
      return;
    }

    if (!filterValue.trim()) {
      return;
    }

    setFilters((prev) => ({
      ...prev,
      [selectedFilterType]: filterValue.trim(),
    }));
    setFilterValue('');
    setPageCursor(undefined); // Reset to first page on filter change
  };

  const handleStatusToggle = (status: TopicStatus) => {
    setFilters((prev) => {
      const currentStatuses = prev.status || [];
      const newStatuses = currentStatuses.includes(status)
        ? currentStatuses.filter((s) => s !== status)
        : [...currentStatuses, status];
      
      return {
        ...prev,
        status: newStatuses.length > 0 ? newStatuses : undefined,
      };
    });
    setPageCursor(undefined); // Reset to first page on filter change
  };

  const handleRemoveFilter = (type: 'name' | 'id' | 'status', value?: string) => {
    setFilters((prev) => {
      if (type === 'status' && value) {
        const newStatuses = (prev.status || []).filter((s) => s !== value);
        return {
          ...prev,
          status: newStatuses.length > 0 ? newStatuses : undefined,
        };
      }
      
      const newFilters = { ...prev };
      delete newFilters[type];
      return newFilters;
    });
    setPageCursor(undefined);
  };

  const handleClearAllFilters = () => {
    setFilters({});
    setFilterValue('');
    setPageCursor(undefined);
  };

  const handleNextPage = () => {
    if (data?.links?.next) {
      // Extract cursor from next link
      const url = new URL(data.links.next, window.location.origin);
      const afterCursor = url.searchParams.get('page[after]');
      if (afterCursor) {
        setPageCursor(`after:${afterCursor}`);
      }
    }
  };

  const handlePrevPage = () => {
    if (data?.links?.prev) {
      // Extract cursor from prev link
      const url = new URL(data.links.prev, window.location.origin);
      const beforeCursor = url.searchParams.get('page[before]');
      const afterCursor = url.searchParams.get('page[after]');
      
      if (beforeCursor) {
        setPageCursor(`before:${beforeCursor}`);
      } else if (afterCursor) {
        setPageCursor(`after:${afterCursor}`);
      } else {
        // If no cursor parameters, go to first page
        setPageCursor(undefined);
      }
    }
  };

  const handlePerPageChange = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number
  ) => {
    setPageSize(newPerPage);
    setPageCursor(undefined); // Reset to first page when page size changes
  };

  if (isLoading) {
    return (
      <PageSection>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h1" size="lg">
            {t('common.loading')}
          </Title>
          <EmptyStateBody>{t('topics.loading')}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection>
        <EmptyState>
          <ExclamationTriangleIcon color="var(--pf-v5-global--danger-color--100)" />
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>
            {error instanceof Error ? error.message : t('topics.errorLoading')}
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {t('topics.title')}
        </Title>
      </PageSection>
      <PageSection>
        <Toolbar>
          <ToolbarContent>
            <ToolbarItem>
              <TopicsFilterToolbar
                selectedFilterType={selectedFilterType}
                onFilterTypeChange={setSelectedFilterType}
                filterValue={filterValue}
                onFilterValueChange={setFilterValue}
                onFilterSubmit={handleFilterSubmit}
                selectedStatuses={filters.status || []}
                onStatusToggle={handleStatusToggle}
              />
            </ToolbarItem>
            <ToolbarItem alignSelf="center">
              <Switch
                label={
                  <>
                    {t('topics.hideInternalTopics')}&nbsp;
                    <Tooltip content={t('topics.hideInternalTopicsTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </>
                }
                isChecked={!includeHidden}
                onChange={(_event, checked) => {
                  setIncludeHidden(!checked);
                  setPageCursor(undefined);
                }}
              />
            </ToolbarItem>
            <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
              <Pagination
                itemCount={totalItems}
                perPage={pageSize}
                page={currentPage}
                onSetPage={() => {}}
                onPerPageSelect={handlePerPageChange}
                onNextClick={handleNextPage}
                onPreviousClick={handlePrevPage}
                isDisabled={false}
                variant={PaginationVariant.top}
                isCompact
                titles={{
                  paginationAriaLabel: t('topics.tableLabel'),
                }}
              />
            </ToolbarItem>
          </ToolbarContent>
          <TopicsFilterChips
            filters={filters}
            onRemoveFilter={handleRemoveFilter}
            onClearAllFilters={handleClearAllFilters}
          />
        </Toolbar>

        {topics.length === 0 ? (
          <EmptyState>
            <CubesIcon />
            <Title headingLevel="h2" size="lg">
              {t('topics.noTopics')}
            </Title>
            <EmptyStateBody>{t('topics.noTopicsDescription')}</EmptyStateBody>
          </EmptyState>
        ) : (
          <>
            <Table aria-label={t('topics.tableLabel')} variant="compact">
              <Thead>
                <Tr>
                  <Th sort={getSortParams('name')}>{t('topics.columnName')}</Th>
                  <Th>{t('topics.columnStatus')}</Th>
                  <Th modifier="fitContent" style={{ textAlign: 'right' }}>{t('topics.columnPartitions')}</Th>
                  <Th modifier="fitContent" style={{ textAlign: 'right' }}>{t('topics.columnGroups')}</Th>
                  <Th sort={getSortParams('totalLeaderLogBytes')} modifier="fitContent" style={{ textAlign: 'right' }}>{t('topics.columnStorage')}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {topics.map((topic) => (
                  <Tr key={topic.id}>
                    <Td dataLabel={t('topics.columnName')}>
                      <Link to={`/kafka/${kafkaId}/topics/${topic.id}`}>
                        {topic.attributes.name}
                      </Link>
                      {topic.meta?.managed === true && <ManagedTopicLabel />}
                    </Td>
                    <Td dataLabel={t('topics.columnStatus')}>
                      {getStatusLabel(topic.attributes.status)}
                    </Td>
                    <Td dataLabel={t('topics.columnPartitions')} modifier="fitContent" style={{ textAlign: 'right' }}>
                      {topic.attributes.numPartitions !== null && topic.attributes.numPartitions !== undefined ? (
                        <Link to={`/kafka/${kafkaId}/topics/${topic.id}/partitions`}>
                          {topic.attributes.numPartitions}
                        </Link>
                      ) : (
                        '-'
                      )}
                    </Td>
                    <Td dataLabel={t('topics.columnGroups')} modifier="fitContent" style={{ textAlign: 'right' }}>
                      {topic.relationships?.groups?.meta?.count !== undefined ? (
                        <Link to={`/kafka/${kafkaId}/topics/${topic.id}/groups`}>
                          {topic.relationships.groups.meta.count}
                        </Link>
                      ) : (
                        topic.relationships?.groups?.meta?.count ?? '-'
                      )}
                    </Td>
                    <Td dataLabel={t('topics.columnStorage')} modifier="fitContent" style={{ textAlign: 'right' }}>
                      {formatBytes(topic.attributes.totalLeaderLogBytes)}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>

            <Toolbar>
              <ToolbarContent>
                <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                  <Pagination
                    itemCount={totalItems}
                    perPage={pageSize}
                    page={currentPage}
                    onSetPage={() => {}}
                    onPerPageSelect={handlePerPageChange}
                    onNextClick={handleNextPage}
                    onPreviousClick={handlePrevPage}
                    isDisabled={false}
                    variant={PaginationVariant.bottom}
                    isCompact
                    titles={{
                      paginationAriaLabel: t('topics.tableLabel'),
                    }}
                  />
                </ToolbarItem>
              </ToolbarContent>
            </Toolbar>
          </>
        )}
      </PageSection>
    </>
  );
}