/**
 * Topic Partitions Tab - Shows partition information for a topic
 */

import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState, useMemo, useEffect } from 'react';
import {
  PageSection,
  EmptyState,
  EmptyStateBody,
  Title,
  Spinner,
  ToggleGroup,
  ToggleGroupItem,
  Icon,
  Label,
  LabelGroup,
  Tooltip,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
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
  ExclamationTriangleIcon,
  ExclamationCircleIcon,
  FlagIcon,
  HelpIcon,
} from '@patternfly/react-icons';
import { useTopic } from '@/api/hooks/useTopics';

type PartitionStatus = 'FullyReplicated' | 'UnderReplicated' | 'Offline';
type FilterType = 'all' | PartitionStatus;
type SortableColumn = 'id' | 'status' | 'leader' | 'preferredLeader' | 'storage';

const StatusLabel: Record<PartitionStatus, { label: React.ReactNode }> = {
  FullyReplicated: {
    label: (
      <>
        <Icon status="success">
          <CheckCircleIcon />
        </Icon>{' '}
        In-sync
      </>
    ),
  },
  UnderReplicated: {
    label: (
      <>
        <Icon status="warning">
          <ExclamationTriangleIcon />
        </Icon>{' '}
        Under replicated
      </>
    ),
  },
  Offline: {
    label: (
      <>
        <Icon status="danger">
          <ExclamationCircleIcon />
        </Icon>{' '}
        Offline
      </>
    ),
  },
};

function formatBytes(bytes: number | undefined | null): string {
  if (bytes === undefined || bytes === null) return 'N/A';
  if (bytes === 0) return '0 B';
  
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

export function TopicPartitionsTab() {
  const { t } = useTranslation();
  const { kafkaId, topicId } = useParams<{ kafkaId: string; topicId: string }>();
  
  const { data, isLoading, error, refetch } = useTopic(kafkaId, topicId, {
    fields: ['name', 'partitions', 'numPartitions'],
  });

  const [filter, setFilter] = useState<FilterType>('all');
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);
  const [sortColumn, setSortColumn] = useState<SortableColumn>('id');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  // Auto-refresh every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      refetch();
    }, 30000);
    return () => clearInterval(interval);
  }, [refetch]);

  const topic = data?.data;
  const partitions = topic?.attributes.partitions || [];

  // Filter and sort partitions
  const filteredAndSortedPartitions = useMemo(() => {
    let filtered = partitions.filter((p) => 
      filter === 'all' ? true : p.status === filter
    );

    // Sort
    filtered = [...filtered].sort((a, b) => {
      let comparison = 0;
      
      switch (sortColumn) {
        case 'id':
          comparison = a.partition - b.partition;
          break;
        case 'leader':
          comparison = (a.leaderId ?? 0) - (b.leaderId ?? 0);
          break;
        case 'status':
          comparison = a.status.localeCompare(b.status);
          break;
        case 'preferredLeader': {
          const apl = a.leaderId === a.replicas[0]?.nodeId;
          const bpl = b.leaderId === b.replicas[0]?.nodeId;
          comparison = Number(apl) - Number(bpl);
          break;
        }
        case 'storage':
          comparison = (a.leaderLocalStorage ?? 0) - (b.leaderLocalStorage ?? 0);
          break;
      }
      
      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return filtered;
  }, [partitions, filter, sortColumn, sortDirection]);

  // Pagination
  const paginatedPartitions = useMemo(() => {
    const startIndex = (page - 1) * perPage;
    return filteredAndSortedPartitions.slice(startIndex, startIndex + perPage);
  }, [filteredAndSortedPartitions, page, perPage]);

  const handleSort = (column: SortableColumn) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const getSortParams = (column: SortableColumn): ThProps['sort'] => ({
    sortBy: {
      index: 0,
      direction: sortColumn === column ? sortDirection : undefined,
    },
    onSort: () => handleSort(column),
    columnIndex: 0,
  });

  // Count partitions by status
  const statusCounts = useMemo(() => ({
    all: partitions.length,
    FullyReplicated: partitions.filter((p) => p.status === 'FullyReplicated').length,
    UnderReplicated: partitions.filter((p) => p.status === 'UnderReplicated').length,
    Offline: partitions.filter((p) => p.status === 'Offline').length,
  }), [partitions]);

  if (isLoading) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  if (!partitions.length) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('topics.partitions.noPartitions')}
          </Title>
          <EmptyStateBody>
            {t('topics.partitions.noPartitionsDescription')}
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  const isFiltered = filter !== 'all';
  const showEmptyState = isFiltered && filteredAndSortedPartitions.length === 0;

  return (
    <PageSection isFilled>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <ToggleGroup aria-label="Filter partitions by state">
              <ToggleGroupItem
                text={`${t('topics.partitions.allPartitions')} (${statusCounts.all})`}
                buttonId="all"
                isSelected={filter === 'all'}
                onChange={() => {
                  setFilter('all');
                  setPage(1);
                }}
              />
              <ToggleGroupItem
                text={
                  <>
                    {StatusLabel.FullyReplicated.label} ({statusCounts.FullyReplicated})
                  </>
                }
                buttonId="in-sync"
                isSelected={filter === 'FullyReplicated'}
                onChange={() => {
                  setFilter('FullyReplicated');
                  setPage(1);
                }}
              />
              <ToggleGroupItem
                text={
                  <>
                    {StatusLabel.UnderReplicated.label} ({statusCounts.UnderReplicated})
                  </>
                }
                buttonId="under-replicated"
                isSelected={filter === 'UnderReplicated'}
                onChange={() => {
                  setFilter('UnderReplicated');
                  setPage(1);
                }}
              />
              <ToggleGroupItem
                text={
                  <>
                    {StatusLabel.Offline.label} ({statusCounts.Offline})
                  </>
                }
                buttonId="offline"
                isSelected={filter === 'Offline'}
                onChange={() => {
                  setFilter('Offline');
                  setPage(1);
                }}
              />
            </ToggleGroup>
          </ToolbarItem>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={filteredAndSortedPartitions.length}
              perPage={perPage}
              page={page}
              onSetPage={(_, newPage) => setPage(newPage)}
              onPerPageSelect={(_, newPerPage) => {
                setPerPage(newPerPage);
                setPage(1);
              }}
              variant="top"
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      {showEmptyState ? (
        <EmptyState>
          <Title headingLevel="h4" size="lg">
            {t('topics.partitions.emptyStateTitle')}
          </Title>
          <EmptyStateBody>
            {t('topics.partitions.emptyStateBody')}
          </EmptyStateBody>
        </EmptyState>
      ) : (
        <>
          <Table ouiaId={"topic-partitions-listing"} aria-label="Partitions table" variant="compact">
            <Thead>
              <Tr>
                <Th width={15} sort={getSortParams('id')}>
                  {t('topics.partitions.partitionId')}
                </Th>
                <Th width={15} sort={getSortParams('status')}>
                  {t('topics.partitions.status')}
                </Th>
                <Th width={20} sort={getSortParams('preferredLeader')}>
                  {t('topics.partitions.preferredLeader')}{' '}
                  <Tooltip content={t('topics.partitions.preferredLeaderTooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </Th>
                <Th width={15} sort={getSortParams('leader')}>
                  {t('topics.partitions.leader')}{' '}
                  <Tooltip content={t('topics.partitions.leaderTooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </Th>
                <Th width={20}>
                  {t('topics.partitions.replicas')}{' '}
                  <Tooltip content={t('topics.partitions.replicasTooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </Th>
                <Th sort={getSortParams('storage')}>
                  {t('topics.partitions.size')}
                </Th>
              </Tr>
            </Thead>
            <Tbody>
              {paginatedPartitions.map((partition) => {
                const leader = partition.replicas.find(
                  (r) => r.nodeId === partition.leaderId
                );
                const isPreferredLeader = partition.leaderId === partition.replicas[0]?.nodeId;

                return (
                  <Tr key={partition.partition}>
                    <Td dataLabel={t('topics.partitions.partitionId')}>
                      {partition.partition}
                    </Td>
                    <Td dataLabel={t('topics.partitions.status')}>
                      {StatusLabel[partition.status].label}
                    </Td>
                    <Td dataLabel={t('topics.partitions.preferredLeader')}>
                      {partition.leaderId !== undefined
                        ? isPreferredLeader
                          ? 'Yes'
                          : 'No'
                        : 'N/A'}
                    </Td>
                    <Td dataLabel={t('topics.partitions.leader')}>
                      {leader ? (
                        <Tooltip
                          content={
                            <>
                              {t('topics.partitions.brokerId')}: {leader.nodeId}
                              <br />
                              {t('topics.partitions.preferredLeader')}
                            </>
                          }
                        >
                          <Label color="teal" isCompact icon={<FlagIcon />}>
                            {leader.nodeId}
                          </Label>
                        </Tooltip>
                      ) : (
                        'N/A'
                      )}
                    </Td>
                    <Td dataLabel={t('topics.partitions.replicas')}>
                      <LabelGroup>
                        {partition.replicas
                          .filter((r) => r.nodeId !== partition.leaderId)
                          .map((r, idx) => (
                            <Tooltip
                              key={idx}
                              content={
                                <>
                                  {t('topics.partitions.brokerId')}: {r.nodeId}
                                  <br />
                                  {t('topics.partitions.replica')}{' '}
                                  {r.inSync
                                    ? t('topics.partitions.inSync')
                                    : t('topics.partitions.underReplicated')}
                                </>
                              }
                            >
                              <Label
                                color={!r.inSync ? 'red' : undefined}
                                isCompact
                                icon={
                                  !r.inSync ? (
                                    <Icon status="warning">
                                      <ExclamationTriangleIcon />
                                    </Icon>
                                  ) : (
                                    <Icon status="success">
                                      <CheckCircleIcon />
                                    </Icon>
                                  )
                                }
                              >
                                {r.nodeId}
                              </Label>
                            </Tooltip>
                          ))}
                      </LabelGroup>
                    </Td>
                    <Td dataLabel={t('topics.partitions.size')}>
                      {formatBytes(partition.leaderLocalStorage)}
                    </Td>
                  </Tr>
                );
              })}
            </Tbody>
          </Table>

          <Toolbar>
            <ToolbarContent>
              <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                <Pagination
                  itemCount={filteredAndSortedPartitions.length}
                  perPage={perPage}
                  page={page}
                  onSetPage={(_, newPage) => setPage(newPage)}
                  onPerPageSelect={(_, newPerPage) => {
                    setPerPage(newPerPage);
                    setPage(1);
                  }}
                  variant="bottom"
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
        </>
      )}
    </PageSection>
  );
}