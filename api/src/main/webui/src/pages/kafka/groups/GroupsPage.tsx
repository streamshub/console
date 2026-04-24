/**
 * Groups Page - List all groups in a Kafka cluster
 */

import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Pagination,
  PaginationVariant,
  SearchInput,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
} from '@patternfly/react-core';
import { useGroups } from '@/api/hooks/useGroups';
import { GroupsTable } from '@/components/kafka/groups/GroupsTable';
import { ResetOffsetModal } from '@/components/kafka/groups/ResetOffset';
import { GroupState, GroupType, Group } from '@/api/types';
import { StatusLabel } from '@/components/StatusLabel';
import { GROUP_STATE_CONFIG } from '@/components/StatusLabel/configs';
import {
  LoadingEmptyState,
  ErrorEmptyState,
  NoDataEmptyState,
  NoResultsEmptyState,
} from '@/components/EmptyStates';
import { useTableState } from '@/hooks';

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

type SortableColumn = 'groupId' | 'type' | 'protocol' | 'state';

export function GroupsPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  // Table state (pagination + sorting)
  const table = useTableState<SortableColumn>({
    initialSortColumn: 'groupId',
    initialSortDirection: 'asc',
  });

  // Filter state
  const [searchValue, setSearchValue] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<GroupType[]>([]);
  const [selectedStates, setSelectedStates] = useState<GroupState[]>([]);
  const [isTypeSelectOpen, setIsTypeSelectOpen] = useState(false);
  const [isStateSelectOpen, setIsStateSelectOpen] = useState(false);

  // Reset offset modal state
  const [isResetOffsetModalOpen, setIsResetOffsetModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);

  // Fetch groups
  const { data, isLoading, error } = useGroups(kafkaId, {
    id: searchValue || undefined,
    type: selectedTypes.length > 0 ? selectedTypes : undefined,
    groupState: selectedStates.length > 0 ? selectedStates : undefined,
    pageSize: table.pageSize,
    pageCursor: table.pageCursor,
    sort: table.sortBy,
    sortDir: table.sortDirection,
  });

  // Update table state when data changes
  useEffect(() => {
    table.setData(data);
  }, [data, table]);

  const groups = data?.data || [];
  const totalItems = data?.meta?.page?.total || 0;
  const currentPage = data?.meta.page.pageNumber || 1;

  const handleSearchChange = (_event: React.FormEvent<HTMLInputElement>, value: string) => {
    setSearchValue(value);
    table.resetPagination();
  };

  const handleTypeToggle = (type: GroupType) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
    table.resetPagination();
  };

  const handleStateToggle = (state: GroupState) => {
    setSelectedStates((prev) =>
      prev.includes(state) ? prev.filter((s) => s !== state) : [...prev, state]
    );
    table.resetPagination();
  };

  const handleResetOffset = (group: Group) => {
    setSelectedGroup(group);
    setIsResetOffsetModalOpen(true);
  };

  const handleCloseResetOffsetModal = () => {
    setIsResetOffsetModalOpen(false);
    setSelectedGroup(null);
  };

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
                  table.resetPagination();
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
                      <StatusLabel status={state} config={GROUP_STATE_CONFIG} />
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
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
                variant={PaginationVariant.top}
                isCompact
              />
            </ToolbarItem>
          </ToolbarContent>
        </Toolbar>

      {isLoading ? (
        <LoadingEmptyState />
      ) : groups.length === 0 ? (
        searchValue || selectedTypes.length > 0 || selectedStates.length > 0 ? (
          <NoResultsEmptyState />
        ) : (
          <NoDataEmptyState
            entityName="groups"
            message="No groups are currently available in this Kafka cluster."
          />
        )
      ) : (
        <>
          <GroupsTable
            groups={groups}
            kafkaId={kafkaId!}
            sortBy={table.sortBy}
            sortDirection={table.sortDirection}
            onSort={table.handleSort}
            onResetOffset={handleResetOffset}
          />
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
                  variant={PaginationVariant.bottom}
                  isCompact
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
        </>
      )}
      </PageSection>

      {selectedGroup && (
        <ResetOffsetModal
          isOpen={isResetOffsetModalOpen}
          onClose={handleCloseResetOffsetModal}
          kafkaId={kafkaId!}
          group={selectedGroup}
        />
      )}
    </>
  );
}