/**
 * Nodes Rebalances Tab - Shows Kafka rebalances
 */

import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  Grid,
  GridItem,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  ToolbarGroup,
  SearchInput,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  Pagination,
  Button,
  EmptyState,
  EmptyStateBody,
  Title,
  PaginationVariant,
} from '@patternfly/react-core';
import { FilterIcon } from '@patternfly/react-icons';
import { useRebalances, usePatchRebalance } from '@/api/hooks/useRebalances';
import { RebalancesTable } from '@/components/kafka/nodes/RebalancesTable';
import { RebalancesCountCard } from '@/components/kafka/nodes/RebalancesCountCard';
import { Rebalance, RebalanceStatus, RebalanceMode } from '@/api/types';
import { useTableState } from '@/hooks';
import { useShowLearning } from '@/hooks/useShowLearning';

export function NodesRebalancesTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const showLearning = useShowLearning();

  // Alert state
  const [isAlertVisible, setIsAlertVisible] = useState(true);

  // Table state (pagination + sorting)
  const table = useTableState<string>({
    initialSortColumn: 'name',
    initialSortDirection: 'asc',
  });

  // Filter state
  const [filterName, setFilterName] = useState('');
  const [filterStatuses, setFilterStatuses] = useState<RebalanceStatus[]>([]);
  const [filterModes, setFilterModes] = useState<RebalanceMode[]>([]);
  const [searchValue, setSearchValue] = useState('');
  const [isStatusSelectOpen, setIsStatusSelectOpen] = useState(false);
  const [isModeSelectOpen, setIsModeSelectOpen] = useState(false);

  // Fetch rebalances with filters
  const { data, isLoading } = useRebalances(kafkaId!, {
    pageSize: table.pageSize,
    pageCursor: table.pageCursor,
    sort: table.sortBy,
    sortDir: table.sortDirection,
    name: filterName || undefined,
    status: filterStatuses.length > 0 ? filterStatuses : undefined,
    mode: filterModes.length > 0 ? filterModes : undefined,
  });

  // Update table state when data changes
  useEffect(() => {
    table.setData(data);
  }, [data, table]);

  // Mutation for rebalance actions
  const { mutate: patchRebalance } = usePatchRebalance(kafkaId!);

  const handleFilterNameChange = (name: string) => {
    setFilterName(name);
    table.resetPagination();
  };

  const handleFilterStatusChange = (statuses: RebalanceStatus[]) => {
    setFilterStatuses(statuses);
    table.resetPagination();
  };

  const handleFilterModeChange = (modes: RebalanceMode[]) => {
    setFilterModes(modes);
    table.resetPagination();
  };

  const handleClearAllFilters = () => {
    setFilterName('');
    setFilterStatuses([]);
    setFilterModes([]);
    table.resetPagination();
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

  const totalCount = data?.meta?.page?.total || 0;
  const page = data?.meta?.page?.pageNumber || 1;

  const allStatuses: RebalanceStatus[] = [
    'New',
    'PendingProposal',
    'ProposalReady',
    'Stopped',
    'Rebalancing',
    'NotReady',
    'Ready',
    'ReconciliationPaused',
  ];

  const allModes: RebalanceMode[] = ['full', 'add-brokers', 'remove-brokers'];

  // Empty state when no rebalances exist
  if (!isLoading && totalCount === 0 && !filterName && filterStatuses.length === 0 && filterModes.length === 0) {
    return (
      <PageSection isFilled>
        <Grid hasGutter>
          {showLearning && isAlertVisible && (
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
            <EmptyState>
              <Title headingLevel="h4" size="lg">
                <FilterIcon /> {t('rebalancing.noRebalances')}
              </Title>
              <EmptyStateBody>{t('rebalancing.noRebalancesDescription')}</EmptyStateBody>
              <Button variant="link">{t('rebalancing.noRebalancesAction')}</Button>
            </EmptyState>
          </GridItem>
        </Grid>
      </PageSection>
    );
  }

  // Empty state when filters don't match
  if (!isLoading && data?.data?.length === 0 && (filterName || filterStatuses.length > 0 || filterModes.length > 0)) {
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
            <EmptyState>
              <Title headingLevel="h4" size="lg">
                <FilterIcon /> {t('common.noResultsFound')}
              </Title>
              <EmptyStateBody>{t('common.noResultsFoundDescription')}</EmptyStateBody>
              <Button variant="link" onClick={handleClearAllFilters}>
                {t('common.clearAllFilters')}
              </Button>
            </EmptyState>
          </GridItem>
        </Grid>
      </PageSection>
    );
  }

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
            totalRebalances={totalCount}
            proposalReady={statusCounts.proposalReady}
            rebalancing={statusCounts.rebalancing}
            ready={statusCounts.ready}
            stopped={statusCounts.stopped}
          />
        </GridItem>
        <GridItem>
          <Toolbar id="rebalances-toolbar">
            <ToolbarContent>
              <ToolbarGroup variant="filter-group">
                <ToolbarItem>
                  <SearchInput
                    placeholder={t('common.filterByName')}
                    value={searchValue}
                    onChange={(_, value) => setSearchValue(value)}
                    onSearch={(_, value) => {
                      handleFilterNameChange(value);
                      setSearchValue(value);
                    }}
                    onClear={() => {
                      handleFilterNameChange('');
                      setSearchValue('');
                    }}
                    aria-label={t('rebalancing.rebalanceName')}
                  />
                </ToolbarItem>

                <ToolbarItem>
                  <Select
                    isOpen={isStatusSelectOpen}
                    onOpenChange={setIsStatusSelectOpen}
                    onSelect={(_, value) => {
                      const status = value as RebalanceStatus;
                      if (filterStatuses.includes(status)) {
                        handleFilterStatusChange(filterStatuses.filter((s) => s !== status));
                      } else {
                        handleFilterStatusChange([...filterStatuses, status]);
                      }
                    }}
                    toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                      <MenuToggle
                        ref={toggleRef}
                        onClick={() => setIsStatusSelectOpen(!isStatusSelectOpen)}
                        isExpanded={isStatusSelectOpen}
                      >
                        {t('rebalancing.status')}
                        {filterStatuses.length > 0 && ` (${filterStatuses.length})`}
                      </MenuToggle>
                    )}
                  >
                    <SelectList>
                      {allStatuses.map((status) => (
                        <SelectOption
                          key={status}
                          value={status}
                          hasCheckbox
                          isSelected={filterStatuses.includes(status)}
                        >
                          {t(`rebalancing.statuses.${status.charAt(0).toLowerCase() + status.slice(1)}.label`)}
                        </SelectOption>
                      ))}
                    </SelectList>
                  </Select>
                </ToolbarItem>

                <ToolbarItem>
                  <Select
                    isOpen={isModeSelectOpen}
                    onOpenChange={setIsModeSelectOpen}
                    onSelect={(_, value) => {
                      const mode = value as RebalanceMode;
                      if (filterModes.includes(mode)) {
                        handleFilterModeChange(filterModes.filter((m) => m !== mode));
                      } else {
                        handleFilterModeChange([...filterModes, mode]);
                      }
                    }}
                    toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                      <MenuToggle
                        ref={toggleRef}
                        onClick={() => setIsModeSelectOpen(!isModeSelectOpen)}
                        isExpanded={isModeSelectOpen}
                      >
                        {t('rebalancing.mode')}
                        {filterModes.length > 0 && ` (${filterModes.length})`}
                      </MenuToggle>
                    )}
                  >
                    <SelectList>
                      {allModes.map((mode) => (
                        <SelectOption
                          key={mode}
                          value={mode}
                          hasCheckbox
                          isSelected={filterModes.includes(mode)}
                        >
                          {mode === 'full'
                            ? t('rebalancing.fullMode')
                            : mode === 'add-brokers'
                              ? t('rebalancing.addBrokersMode')
                              : t('rebalancing.removeBrokersMode')}
                        </SelectOption>
                      ))}
                    </SelectList>
                  </Select>
                </ToolbarItem>

                {(filterName || filterStatuses.length > 0 || filterModes.length > 0) && (
                  <ToolbarItem>
                    <Button variant="link" onClick={handleClearAllFilters}>
                      {t('common.clearAllFilters')}
                    </Button>
                  </ToolbarItem>
                )}
              </ToolbarGroup>

              <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                <Pagination
                  itemCount={totalCount}
                  perPage={table.pageSize}
                  page={page}
                  onSetPage={() => {}}
                  onPerPageSelect={table.handlePerPageChange}
                  onNextClick={table.handleNextPage}
                  onPreviousClick={table.handlePrevPage}
                  variant={PaginationVariant.top}
                  isCompact
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>

          <RebalancesTable
            rebalances={data?.data}
            sortBy={table.sortBy || 'name'}
            sortDirection={table.sortDirection || 'asc'}
            onSort={table.handleSort}
            onApprove={handleApprove}
            onStop={handleStop}
            onRefresh={handleRefresh}
            kafkaId={kafkaId!}
          />

          <Toolbar>
            <ToolbarContent>
              <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                <Pagination
                  itemCount={totalCount}
                  perPage={table.pageSize}
                  page={page}
                  onSetPage={() => {}}
                  onPerPageSelect={table.handlePerPageChange}
                  onNextClick={table.handleNextPage}
                  onPreviousClick={table.handlePrevPage}
                  variant={PaginationVariant.bottom}
                  isCompact
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
        </GridItem>
      </Grid>
    </PageSection>
  );
}