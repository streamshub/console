/**
 * Groups Page - List all groups in a Kafka cluster
 */

import { useState } from 'react';
import { useParams } from 'react-router-dom';
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
  SearchInput,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  Icon,
} from '@patternfly/react-core';
import {
  SearchIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  InfoCircleIcon,
  PendingIcon,
  HistoryIcon,
  OffIcon,
  SyncAltIcon,
  InProgressIcon,
} from '@patternfly/react-icons';
import { useGroups } from '../api/hooks/useGroups';
import { GroupsTable } from '../components/GroupsTable';
import { GroupState, GroupType } from '../api/types';

const GROUP_TYPES: GroupType[] = ['Classic', 'Consumer', 'Share', 'Streams'];

const GROUP_STATES: GroupState[] = [
  'STABLE',
  'EMPTY',
  'DEAD',
  'PREPARING_REBALANCE',
  'COMPLETING_REBALANCE',
  'ASSIGNING',
  'RECONCILING',
];

const STATE_LABELS: Record<GroupState, { label: React.ReactNode }> = {
  STABLE: {
    label: (
      <>
        <Icon status="success">
          <CheckCircleIcon />
        </Icon>
        &nbsp;Stable
      </>
    ),
  },
  EMPTY: {
    label: (
      <>
        <Icon status="info">
          <InfoCircleIcon />
        </Icon>
        &nbsp;Empty
      </>
    ),
  },
  UNKNOWN: {
    label: (
      <>
        <Icon status="warning">
          <ExclamationTriangleIcon />
        </Icon>
        &nbsp;Unknown
      </>
    ),
  },
  PREPARING_REBALANCE: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp;Preparing Rebalance
      </>
    ),
  },
  ASSIGNING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Assigning
      </>
    ),
  },
  DEAD: {
    label: (
      <>
        <Icon>
          <OffIcon />
        </Icon>
        &nbsp;Dead
      </>
    ),
  },
  COMPLETING_REBALANCE: {
    label: (
      <>
        <Icon>
          <SyncAltIcon />
        </Icon>
        &nbsp;Completing Rebalance
      </>
    ),
  },
  RECONCILING: {
    label: (
      <>
        <Icon>
          <InProgressIcon />
        </Icon>
        &nbsp;Reconciling
      </>
    ),
  },
};

export function GroupsPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  // Pagination state
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);
  const [pageCursor, setPageCursor] = useState<string | undefined>(undefined);

  // Filter state
  const [searchValue, setSearchValue] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<GroupType[]>([]);
  const [selectedStates, setSelectedStates] = useState<GroupState[]>([]);
  const [isTypeSelectOpen, setIsTypeSelectOpen] = useState(false);
  const [isStateSelectOpen, setIsStateSelectOpen] = useState(false);

  // Sorting state
  const [sortBy, setSortBy] = useState<'groupId' | 'type' | 'protocol' | 'state'>('groupId');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  // Fetch groups
  const { data, isLoading, error } = useGroups(kafkaId, {
    id: searchValue || undefined,
    type: selectedTypes.length > 0 ? selectedTypes : undefined,
    groupState: selectedStates.length > 0 ? selectedStates : undefined,
    pageSize: perPage,
    pageCursor,
    sort: sortBy,
    sortDir: sortDirection,
  });

  const groups = data?.data || [];
  const totalItems = data?.meta?.page?.total || 0;

  const handlePageChange = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setPage(newPage);
    // Calculate cursor based on page navigation
    if (newPage > page && data?.links?.next) {
      const nextCursor = new URLSearchParams(data.links.next).get('page[after]');
      setPageCursor(nextCursor ? `after:${nextCursor}` : undefined);
    } else if (newPage < page && data?.links?.prev) {
      const prevCursor = new URLSearchParams(data.links.prev).get('page[before]');
      setPageCursor(prevCursor ? `before:${prevCursor}` : undefined);
    } else {
      setPageCursor(undefined);
    }
  };

  const handlePerPageChange = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setPerPage(newPerPage);
    setPage(1);
    setPageCursor(undefined);
  };

  const handleSearchChange = (_event: React.FormEvent<HTMLInputElement>, value: string) => {
    setSearchValue(value);
    setPage(1);
    setPageCursor(undefined);
  };

  const handleTypeToggle = (type: GroupType) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
    setPage(1);
    setPageCursor(undefined);
  };

  const handleStateToggle = (state: GroupState) => {
    setSelectedStates((prev) =>
      prev.includes(state) ? prev.filter((s) => s !== state) : [...prev, state]
    );
    setPage(1);
    setPageCursor(undefined);
  };

  const handleSort = (column: 'groupId' | 'type' | 'protocol' | 'state') => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
    setPage(1);
    setPageCursor(undefined);
  };

  if (error) {
    return (
      <PageSection>
        <EmptyState>
          <ExclamationTriangleIcon />
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {t('groups.title')}
        </Title>
      </PageSection>
      <PageSection>
        <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <SearchInput
              placeholder={t('common.filterByName')}
              value={searchValue}
              onChange={handleSearchChange}
              onClear={() => {
                setSearchValue('');
                setPage(1);
                setPageCursor(undefined);
              }}
            />
          </ToolbarItem>

          <ToolbarItem>
            <Select
              id="type-select"
              isOpen={isTypeSelectOpen}
              selected={selectedTypes}
              onSelect={(_event, value) => handleTypeToggle(value as GroupType)}
              onOpenChange={(isOpen) => setIsTypeSelectOpen(isOpen)}
              toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                <MenuToggle
                  ref={toggleRef}
                  onClick={() => setIsTypeSelectOpen(!isTypeSelectOpen)}
                  isExpanded={isTypeSelectOpen}
                >
                  {selectedTypes.length > 0
                    ? `${t('groups.type')} (${selectedTypes.length})`
                    : t('groups.type')}
                </MenuToggle>
              )}
            >
              <SelectList>
                {GROUP_TYPES.map((type) => (
                  <SelectOption
                    key={type}
                    value={type}
                    hasCheckbox
                    isSelected={selectedTypes.includes(type)}
                  >
                    {type}
                  </SelectOption>
                ))}
              </SelectList>
            </Select>
          </ToolbarItem>

          <ToolbarItem>
            <Select
              id="state-select"
              isOpen={isStateSelectOpen}
              selected={selectedStates}
              onSelect={(_event, value) => handleStateToggle(value as GroupState)}
              onOpenChange={(isOpen) => setIsStateSelectOpen(isOpen)}
              toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                <MenuToggle
                  ref={toggleRef}
                  onClick={() => setIsStateSelectOpen(!isStateSelectOpen)}
                  isExpanded={isStateSelectOpen}
                >
                  {selectedStates.length > 0
                    ? `${t('groups.state')} (${selectedStates.length})`
                    : t('groups.state')}
                </MenuToggle>
              )}
            >
              <SelectList>
                {GROUP_STATES.map((state) => (
                  <SelectOption
                    key={state}
                    value={state}
                    hasCheckbox
                    isSelected={selectedStates.includes(state)}
                  >
                    {STATE_LABELS[state].label}
                  </SelectOption>
                ))}
              </SelectList>
            </Select>
          </ToolbarItem>

          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalItems}
              perPage={perPage}
              page={page}
              onSetPage={handlePageChange}
              onPerPageSelect={handlePerPageChange}
              variant={PaginationVariant.top}
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      {isLoading ? (
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      ) : groups.length === 0 ? (
        <EmptyState>
          <SearchIcon />
          <Title headingLevel="h2" size="lg">
            {t('groups.noGroups')}
          </Title>
          <EmptyStateBody>
            {searchValue || selectedTypes.length > 0 || selectedStates.length > 0
              ? t('common.noResultsFoundDescription')
              : 'No groups are currently available in this Kafka cluster.'}
          </EmptyStateBody>
        </EmptyState>
      ) : (
        <>
          <GroupsTable
            groups={groups}
            kafkaId={kafkaId!}
            sortBy={sortBy}
            sortDirection={sortDirection}
            onSort={handleSort}
          />
          <Toolbar>
            <ToolbarContent>
              <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                <Pagination
                  itemCount={totalItems}
                  perPage={perPage}
                  page={page}
                  onSetPage={handlePageChange}
                  onPerPageSelect={handlePerPageChange}
                  variant={PaginationVariant.bottom}
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