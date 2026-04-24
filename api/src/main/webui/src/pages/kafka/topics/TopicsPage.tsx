/**
 * Topics Page - List all topics in a Kafka cluster
 */

import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Spinner,
  Pagination,
  PaginationVariant,
  Switch,
  Tooltip,
  Label,
  Split,
  SplitItem,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from '@patternfly/react-table';
import {
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ExclamationCircleIcon,
} from '@patternfly/react-icons';
import { useTopics } from '@/api/hooks/useTopics';
import { formatBytes } from '@/utils/format';
import {
  TopicsFilterToolbar,
  TopicsFilterChips,
  TopicsFilterState,
  TopicsFilterType,
  TopicStatus,
} from '@/components/kafka/topics/TopicsFilter';
import { ManagedTopicLabel } from '@/components/kafka/topics/ManagedTopicLabel';
import { StatusLabel } from '@/components/StatusLabel';
import { TOPIC_STATUS_CONFIG } from '@/components/StatusLabel/configs';
import {
  LoadingEmptyState,
  ErrorEmptyState,
  NoDataEmptyState,
  NoResultsEmptyState,
} from '@/components/EmptyStates';
import { useTableState } from '@/hooks';

type SortableColumn = 'name' | 'totalLeaderLogBytes';

export function TopicsPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  
  const [includeHidden, setIncludeHidden] = useState(false);

  // Advanced filtering state
  const [selectedFilterType, setSelectedFilterType] = useState<TopicsFilterType>('name');
  const [filterValue, setFilterValue] = useState('');
  const [filters, setFilters] = useState<TopicsFilterState>({});

  // Table state: pagination + sorting (call BEFORE data fetching)
  const table = useTableState<SortableColumn>({
    initialSortColumn: 'name',
    initialSortDirection: 'asc',
  });

  // Fetch topics data
  const { data, isLoading, error } = useTopics(kafkaId, {
    pageSize: table.pageSize,
    pageCursor: table.pageCursor,
    sort: table.sortBy,
    sortDir: table.sortDirection,
    name: filters.name,
    id: filters.id,
    status: filters.status,
    includeHidden,
    fields: ['name', 'status', 'numPartitions', 'totalLeaderLogBytes', 'groups'],
  });

  // Update table state with response data
  useEffect(() => {
    table.setData(data);
  }, [data, table]);

  const topics = data?.data || [];
  const totalItems = data?.meta.page.total || 0;
  const currentPage = data?.meta.page.pageNumber || 1;
  const statusSummary = data?.meta.summary?.statuses;


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
    table.resetPagination(); // Reset to first page on filter change
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
    table.resetPagination(); // Reset to first page on filter change
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
    table.resetPagination();
  };

  const handleClearAllFilters = () => {
    setFilters({});
    setFilterValue('');
    table.resetPagination();
  };

  if (isLoading) {
    return (
      <PageSection>
        <LoadingEmptyState message={t('topics.loading')} />
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection>
        <ErrorEmptyState error={error} />
      </PageSection>
    );
  }

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          <Split hasGutter style={{ alignItems: 'center', display: 'flex' }}>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>{t('topics.title')}</SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Label icon={isLoading ? <Spinner size="sm" /> : undefined}>
                {totalItems}&nbsp;total
              </Label>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('topics.statusLabels.fullyReplicatedTooltip', 'Number of topics in sync')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <CheckCircleIcon />}
                  color="green"
                >
                  {statusSummary?.FullyReplicated ?? 0}
                </Label>
              </Tooltip>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('topics.statusLabels.underReplicatedTooltip', 'Number of topics under replicated')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <ExclamationTriangleIcon />}
                  color="orange"
                >
                  {(statusSummary?.UnderReplicated ?? 0) + (statusSummary?.PartiallyOffline ?? 0) + (statusSummary?.Unknown ?? 0)}
                </Label>
              </Tooltip>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('topics.statusLabels.offlineTooltip', 'Number of topics not available')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <ExclamationCircleIcon />}
                  color="red"
                >
                  {statusSummary?.Offline ?? 0}
                </Label>
              </Tooltip>
            </SplitItem>
          </Split>
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
                  table.resetPagination();
                }}
              />
            </ToolbarItem>
            <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
              <Pagination
                itemCount={totalItems}
                perPage={table.pageSize}
                page={currentPage}
                onSetPage={() => {}}
                onPerPageSelect={table.handlePerPageChange}
                onNextClick={table.handleNextPage}
                onPreviousClick={table.handlePrevPage}
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
          filters.name || filters.id || filters.status ? (
            <NoResultsEmptyState />
          ) : (
            <NoDataEmptyState
              entityName="topics"
              message={t('topics.noTopicsDescription')}
            />
          )
        ) : (
          <>
            <Table ouiaId={"topics-listing"} aria-label={t('topics.tableLabel')} variant="compact">
              <Thead>
                <Tr>
                  <Th sort={table.getSortParams('name')}>{t('topics.columnName')}</Th>
                  <Th>{t('topics.columnStatus')}</Th>
                  <Th modifier="fitContent" style={{ textAlign: 'right' }}>{t('topics.columnPartitions')}</Th>
                  <Th modifier="fitContent" style={{ textAlign: 'right' }}>{t('topics.columnGroups')}</Th>
                  <Th sort={table.getSortParams('totalLeaderLogBytes')} modifier="fitContent" style={{ textAlign: 'right' }}>{t('topics.columnStorage')}</Th>
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
                      {topic.attributes.status && (
                        <StatusLabel status={topic.attributes.status} config={TOPIC_STATUS_CONFIG} />
                      )}
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
                    perPage={table.pageSize}
                    page={currentPage}
                    onSetPage={() => {}}
                    onPerPageSelect={table.handlePerPageChange}
                    onNextClick={table.handleNextPage}
                    onPreviousClick={table.handlePrevPage}
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