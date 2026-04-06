/**
 * Rebalances Table Component
 * Displays Kafka rebalances with actions (approve, stop, refresh)
 */

import { useState, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { formatDateTime } from '../utils/dateTime';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ActionsColumn,
  ExpandableRowContent,
  ThProps,
} from '@patternfly/react-table';
import {
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
  EmptyState,
  EmptyStateBody,
  Button,
  Flex,
  FlexItem,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Badge,
  Popover,
  List,
  ListItem,
  Tooltip,
  Icon,
  Title,
} from '@patternfly/react-core';
import {
  CheckIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  OutlinedClockIcon,
  HelpIcon,
  FilterIcon,
} from '@patternfly/react-icons';
import { Rebalance, RebalanceStatus, RebalanceMode } from '../api/types';
import { RebalanceModal } from './RebalanceModal';

// Status label components with icons and tooltips
const useRebalanceStatusLabels = (): Record<RebalanceStatus, ReactNode> => {
  const { t } = useTranslation();

  return {
    New: (
      <Tooltip content={t('rebalancing.statuses.new.tooltip')}>
        <span>
          <Icon>
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.new.label')}
        </span>
      </Tooltip>
    ),
    PendingProposal: (
      <Tooltip content={t('rebalancing.statuses.pendingProposal.tooltip')}>
        <span>
          <Icon>
            <PendingIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.pendingProposal.label')}
        </span>
      </Tooltip>
    ),
    ProposalReady: (
      <Tooltip content={t('rebalancing.statuses.proposalReady.tooltip')}>
        <span>
          <Icon>
            <CheckIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.proposalReady.label')}
        </span>
      </Tooltip>
    ),
    Stopped: (
      <Tooltip content={t('rebalancing.statuses.stopped.tooltip')}>
        <span>
          <Icon>
            <img src="/stop-icon.svg" alt="stop icon" width="16" height="16" />
          </Icon>
          &nbsp;{t('rebalancing.statuses.stopped.label')}
        </span>
      </Tooltip>
    ),
    Rebalancing: (
      <Tooltip content={t('rebalancing.statuses.rebalancing.tooltip')}>
        <span>
          <Icon>
            <PendingIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.rebalancing.label')}
        </span>
      </Tooltip>
    ),
    NotReady: (
      <Tooltip content={t('rebalancing.statuses.notReady.tooltip')}>
        <span>
          <Icon>
            <OutlinedClockIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.notReady.label')}
        </span>
      </Tooltip>
    ),
    Ready: (
      <Tooltip content={t('rebalancing.statuses.ready.tooltip')}>
        <span>
          <Icon>
            <CheckIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.ready.label')}
        </span>
      </Tooltip>
    ),
    ReconciliationPaused: (
      <Tooltip content={t('rebalancing.statuses.reconciliationPaused.tooltip')}>
        <span>
          <Icon>
            <PauseCircleIcon />
          </Icon>
          &nbsp;{t('rebalancing.statuses.reconciliationPaused.label')}
        </span>
      </Tooltip>
    ),
  };
};

interface RebalancesTableProps {
  rebalances: Rebalance[] | undefined;
  isLoading: boolean;
  totalCount: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number, pageSize: number) => void;
  sortBy: string;
  sortDirection: 'asc' | 'desc';
  onSort: (column: string) => void;
  filterName: string;
  filterStatuses: RebalanceStatus[];
  filterModes: RebalanceMode[];
  onFilterNameChange: (name: string) => void;
  onFilterStatusChange: (statuses: RebalanceStatus[]) => void;
  onFilterModeChange: (modes: RebalanceMode[]) => void;
  onClearAllFilters: () => void;
  onApprove: (rebalance: Rebalance) => void;
  onStop: (rebalance: Rebalance) => void;
  onRefresh: (rebalance: Rebalance) => void;
  kafkaId: string;
}

export function RebalancesTable({
  rebalances,
  isLoading,
  totalCount,
  page,
  pageSize,
  onPageChange,
  sortBy,
  sortDirection,
  onSort,
  filterName,
  filterStatuses,
  filterModes,
  onFilterNameChange,
  onFilterStatusChange,
  onFilterModeChange,
  onClearAllFilters,
  onApprove,
  onStop,
  onRefresh,
  kafkaId,
}: RebalancesTableProps) {
  const { t } = useTranslation();
  const statusLabels = useRebalanceStatusLabels();

  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const [isStatusSelectOpen, setIsStatusSelectOpen] = useState(false);
  const [isModeSelectOpen, setIsModeSelectOpen] = useState(false);
  const [searchValue, setSearchValue] = useState(filterName);
  const [selectedRebalance, setSelectedRebalance] = useState<Rebalance | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleRebalanceClick = (rebalance: Rebalance) => {
    setSelectedRebalance(rebalance);
    setIsModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedRebalance(null);
  };

  const toggleRowExpanded = (id: string) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const getSortParams = (columnName: string): ThProps['sort'] => ({
    sortBy: {
      index: sortBy === columnName ? 0 : undefined,
      direction: sortDirection,
    },
    onSort: () => onSort(columnName),
    columnIndex: 0,
  });

  // Get last updated timestamp
  const getLastUpdated = (rebalance: Rebalance): string => {
    const statusCondition = rebalance.attributes.conditions?.find(
      (c) => c.type === rebalance.attributes.status
    );
    return statusCondition?.lastTransitionTime || rebalance.attributes.creationTimestamp || '';
  };

  // Empty state when no rebalances exist
  if (!isLoading && totalCount === 0 && !filterName && filterStatuses.length === 0 && filterModes.length === 0) {
    return (
      <EmptyState>
        <Title headingLevel="h4" size="lg">
          <FilterIcon /> {t('rebalancing.noRebalances')}
        </Title>
        <EmptyStateBody>{t('rebalancing.noRebalancesDescription')}</EmptyStateBody>
        <Button variant="link">{t('rebalancing.noRebalancesAction')}</Button>
      </EmptyState>
    );
  }

  // Empty state when filters don't match
  if (!isLoading && rebalances?.length === 0 && (filterName || filterStatuses.length > 0 || filterModes.length > 0)) {
    return (
      <EmptyState>
        <Title headingLevel="h4" size="lg">
          <FilterIcon /> {t('common.noResultsFound')}
        </Title>
        <EmptyStateBody>{t('common.noResultsFoundDescription')}</EmptyStateBody>
        <Button variant="link" onClick={onClearAllFilters}>
          {t('common.clearAllFilters')}
        </Button>
      </EmptyState>
    );
  }

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

  return (
    <>
      <Toolbar id="rebalances-toolbar">
        <ToolbarContent>
          <ToolbarGroup variant="filter-group">
            <ToolbarItem>
              <SearchInput
                placeholder={t('common.filterByName')}
                value={searchValue}
                onChange={(_, value) => setSearchValue(value)}
                onSearch={(_, value) => {
                  onFilterNameChange(value);
                  setSearchValue(value);
                }}
                onClear={() => {
                  onFilterNameChange('');
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
                    onFilterStatusChange(filterStatuses.filter((s) => s !== status));
                  } else {
                    onFilterStatusChange([...filterStatuses, status]);
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
                    onFilterModeChange(filterModes.filter((m) => m !== mode));
                  } else {
                    onFilterModeChange([...filterModes, mode]);
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
                <Button variant="link" onClick={onClearAllFilters}>
                  {t('common.clearAllFilters')}
                </Button>
              </ToolbarItem>
            )}
          </ToolbarGroup>

          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalCount}
              page={page}
              perPage={pageSize}
              onSetPage={(_, newPage) => onPageChange(newPage, pageSize)}
              onPerPageSelect={(_, newPageSize) => onPageChange(1, newPageSize)}
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      <Table aria-label={t('rebalancing.title')} variant="compact">
        <Thead>
          <Tr>
            <Th />
            <Th width={30} sort={getSortParams('name')}>
              {t('rebalancing.rebalanceName')}
            </Th>
            <Th sort={getSortParams('status')}>{t('rebalancing.status')}</Th>
            <Th sort={getSortParams('lastUpdated')}>{t('rebalancing.lastUpdated')}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {rebalances?.map((rebalance) => {
            const isExpanded = expandedRows.has(rebalance.id);
            const lastUpdated = getLastUpdated(rebalance);

            return (
              <>
                <Tr key={rebalance.id} style={{ verticalAlign: 'middle' }}>
                  <Td
                    expand={{
                      rowIndex: 0,
                      isExpanded,
                      onToggle: () => toggleRowExpanded(rebalance.id),
                    }}
                  />
                  <Td dataLabel={t('rebalancing.rebalanceName')}>
                    <Button
                      variant="link"
                      isInline
                      onClick={() => handleRebalanceClick(rebalance)}
                    >
                      <Badge>{t('rebalancing.crBadge')}</Badge> {rebalance.attributes.name}
                    </Button>
                  </Td>
                  <Td dataLabel={t('rebalancing.status')}>
                    {rebalance.attributes.status ? statusLabels[rebalance.attributes.status] : statusLabels.New}
                  </Td>
                  <Td dataLabel={t('rebalancing.lastUpdated')}>
                    {formatDateTime({ value: lastUpdated })}
                  </Td>
                  <Td isActionCell>
                    <ActionsColumn
                      items={[
                        {
                          title: t('rebalancing.approve'),
                          onClick: () => onApprove(rebalance),
                          isDisabled: !rebalance.meta?.allowedActions?.includes('approve'),
                        },
                        {
                          title: t('rebalancing.refresh'),
                          onClick: () => onRefresh(rebalance),
                          isDisabled: !rebalance.meta?.allowedActions?.includes('refresh'),
                        },
                        {
                          title: t('rebalancing.stop'),
                          onClick: () => onStop(rebalance),
                          isDisabled: !rebalance.meta?.allowedActions?.includes('stop'),
                        },
                      ]}
                    />
                  </Td>
                </Tr>
                {isExpanded && (
                  <Tr isExpanded={isExpanded}>
                    <Td colSpan={5}>
                      <ExpandableRowContent>
                        <DescriptionList className="pf-v6-u-mt-md pf-v6-u-mb-lg">
                          <Flex justifyContent={{ default: 'justifyContentSpaceEvenly' }}>
                            <FlexItem style={{ width: '25%' }}>
                              <DescriptionListGroup>
                                <DescriptionListTerm>{t('rebalancing.autoApprovalEnabled')}</DescriptionListTerm>
                                <DescriptionListDescription>
                                  {rebalance.meta?.autoApproval === true ? 'true' : 'false'}
                                </DescriptionListDescription>
                              </DescriptionListGroup>
                            </FlexItem>
                            <FlexItem style={{ width: '50%', paddingRight: '5rem' }}>
                              <DescriptionListGroup>
                                <DescriptionListTerm>
                                  {t('rebalancing.mode')}{' '}
                                  <Popover
                                    aria-label={t('rebalancing.mode')}
                                    headerContent={<div>{t('rebalancing.rebalanceMode')}</div>}
                                    bodyContent={
                                      <div>
                                        <List>
                                          <ListItem>
                                            <strong>{t('rebalancing.fullMode')}</strong>{' '}
                                            {t('rebalancing.fullModeDescription')}
                                          </ListItem>
                                          <ListItem>
                                            <strong>{t('rebalancing.addBrokersMode')}</strong>{' '}
                                            {t('rebalancing.addBrokersModeDescription')}
                                          </ListItem>
                                          <ListItem>
                                            <strong>{t('rebalancing.removeBrokersMode')}</strong>{' '}
                                            {t('rebalancing.removeBrokersModeDescription')}
                                          </ListItem>
                                        </List>
                                      </div>
                                    }
                                  >
                                    <HelpIcon />
                                  </Popover>
                                </DescriptionListTerm>
                                <DescriptionListDescription>
                                  {rebalance.attributes.mode === 'full' ? (
                                    t('rebalancing.fullMode')
                                  ) : (
                                    <>
                                      {rebalance.attributes.mode === 'add-brokers'
                                        ? t('rebalancing.addBrokersMode')
                                        : t('rebalancing.removeBrokersMode')}{' '}
                                      {rebalance.attributes.brokers?.length
                                        ? rebalance.attributes.brokers.map((b, index) => (
                                            <span key={b}>
                                              <Link to={`/kafka/${kafkaId}/nodes/${b}`}>
                                                {t('rebalancing.broker', { b })}
                                              </Link>
                                              {index < (rebalance.attributes.brokers?.length || 0) - 1 && ', '}
                                            </span>
                                          ))
                                        : ''}
                                    </>
                                  )}
                                </DescriptionListDescription>
                              </DescriptionListGroup>
                            </FlexItem>
                          </Flex>
                        </DescriptionList>
                      </ExpandableRowContent>
                    </Td>
                  </Tr>
                )}
              </>
            );
          })}
        </Tbody>
      </Table>

      <Toolbar>
        <ToolbarContent>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalCount}
              page={page}
              perPage={pageSize}
              onSetPage={(_, newPage) => onPageChange(newPage, pageSize)}
              onPerPageSelect={(_, newPageSize) => onPageChange(1, newPageSize)}
              variant="bottom"
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      <RebalanceModal
        rebalance={selectedRebalance}
        isOpen={isModalOpen}
        onClose={handleModalClose}
      />
    </>
  );
}