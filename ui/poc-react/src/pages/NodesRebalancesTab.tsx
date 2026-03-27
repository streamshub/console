/**
 * Nodes Rebalances Tab - Shows Kafka rebalances
 */

import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  Grid,
  GridItem,
} from '@patternfly/react-core';
import { useRebalances, usePatchRebalance } from '../api/hooks/useRebalances';
import { RebalancesTable } from '../components/RebalancesTable';
import { RebalancesCountCard } from '../components/RebalancesCountCard';
import { Rebalance, RebalanceStatus, RebalanceMode } from '../api/types';

export function NodesRebalancesTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  // Alert state
  const [isAlertVisible, setIsAlertVisible] = useState(true);

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Sorting state
  const [sortBy, setSortBy] = useState('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  // Filter state
  const [filterName, setFilterName] = useState('');
  const [filterStatuses, setFilterStatuses] = useState<RebalanceStatus[]>([]);
  const [filterModes, setFilterModes] = useState<RebalanceMode[]>([]);

  // Fetch rebalances with filters
  const { data, isLoading } = useRebalances(kafkaId!, {
    pageSize,
    pageCursor: page > 1 ? `after:${(page - 1) * pageSize}` : undefined,
    sort: sortDirection === 'desc' ? `-${sortBy}` : sortBy,
    name: filterName || undefined,
    status: filterStatuses.length > 0 ? filterStatuses : undefined,
    mode: filterModes.length > 0 ? filterModes : undefined,
  });

  // Mutation for rebalance actions
  const { mutate: patchRebalance } = usePatchRebalance(kafkaId!);

  const handlePageChange = (newPage: number, newPageSize: number) => {
    setPage(newPage);
    setPageSize(newPageSize);
  };

  const handleSort = (column: string) => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
  };

  const handleFilterNameChange = (name: string) => {
    setFilterName(name);
    setPage(1); // Reset to first page when filter changes
  };

  const handleFilterStatusChange = (statuses: RebalanceStatus[]) => {
    setFilterStatuses(statuses);
    setPage(1);
  };

  const handleFilterModeChange = (modes: RebalanceMode[]) => {
    setFilterModes(modes);
    setPage(1);
  };

  const handleClearAllFilters = () => {
    setFilterName('');
    setFilterStatuses([]);
    setFilterModes([]);
    setPage(1);
  };

  const handleApprove = (rebalance: Rebalance) => {
    patchRebalance({
      rebalanceId: rebalance.id,
      action: 'approve',
    });
  };

  const handleStop = (rebalance: Rebalance) => {
    patchRebalance({
      rebalanceId: rebalance.id,
      action: 'stop',
    });
  };

  const handleRefresh = (rebalance: Rebalance) => {
    patchRebalance({
      rebalanceId: rebalance.id,
      action: 'refresh',
    });
  };

  // Calculate status counts
  const statusCounts = data?.data?.reduce(
    (acc, rebalance) => {
      const status = rebalance.attributes.status;
      if (status === 'ProposalReady') acc.proposalReady += 1;
      if (status === 'Rebalancing') acc.rebalancing += 1;
      if (status === 'Ready') acc.ready += 1;
      if (status === 'Stopped') acc.stopped += 1;
      return acc;
    },
    { proposalReady: 0, rebalancing: 0, ready: 0, stopped: 0 }
  ) || { proposalReady: 0, rebalancing: 0, ready: 0, stopped: 0 };

  return (
    <PageSection isFilled>
      <Grid hasGutter>
        {isAlertVisible && (
          <GridItem>
            <Alert
              variant="info"
              isInline
              title={t('rebalancing.cruiseControlEnabled')}
              actionClose={<AlertActionCloseButton onClose={() => setIsAlertVisible(false)} />}
              actionLinks={
                <AlertActionLink
                  component="a"
                  href={t('rebalancing.cruiseControlLink')}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {t('rebalancing.learnMoreAboutCruiseControl')}
                </AlertActionLink>
              }
            />
          </GridItem>
        )}
        <GridItem>
          <RebalancesCountCard
            totalRebalances={data?.meta?.page?.total || 0}
            proposalReady={statusCounts.proposalReady}
            rebalancing={statusCounts.rebalancing}
            ready={statusCounts.ready}
            stopped={statusCounts.stopped}
          />
        </GridItem>
        <GridItem>
          <RebalancesTable
        rebalances={data?.data}
        isLoading={isLoading}
        totalCount={data?.meta?.page?.total || 0}
        page={page}
        pageSize={pageSize}
        onPageChange={handlePageChange}
        sortBy={sortBy}
        sortDirection={sortDirection}
        onSort={handleSort}
        filterName={filterName}
        filterStatuses={filterStatuses}
        filterModes={filterModes}
        onFilterNameChange={handleFilterNameChange}
        onFilterStatusChange={handleFilterStatusChange}
        onFilterModeChange={handleFilterModeChange}
        onClearAllFilters={handleClearAllFilters}
        onApprove={handleApprove}
        onStop={handleStop}
        onRefresh={handleRefresh}
        kafkaId={kafkaId!}
          />
        </GridItem>
      </Grid>
    </PageSection>
  );
}