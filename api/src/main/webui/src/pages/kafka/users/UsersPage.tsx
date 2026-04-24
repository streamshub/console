/**
 * Users Page - List all Kafka users in a cluster
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
} from '@patternfly/react-core';
import { useUsers } from '@/api/hooks/useUsers';
import { UsersTable } from '@/components/kafka/users/UsersTable';
import { LoadingEmptyState, ErrorEmptyState } from '@/components/EmptyStates';
import { useTableState } from '@/hooks';

type SortableColumn = 'name' | 'namespace' | 'creationTimestamp' | 'username' | 'authenticationType';

export function UsersPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  const [usernameFilter, setUsernameFilter] = useState('');

  // Table state: pagination + sorting
  const table = useTableState<SortableColumn>({
    initialSortColumn: 'name',
    initialSortDirection: 'asc',
  });

  const { data, isLoading, error } = useUsers(kafkaId, {
    pageSize: table.pageSize,
    pageCursor: table.pageCursor,
    sort: table.sortBy,
    sortDir: table.sortDirection,
    username: usernameFilter || undefined,
  });

  // Update table state with response data
  useEffect(() => {
    table.setData(data);
  }, [data, table]);

  const users = data?.data || [];
  const totalItems = data?.meta.page.total || 0;
  const currentPage = data?.meta.page.pageNumber || 1;

  const handleUsernameFilterChange = (value: string) => {
    setUsernameFilter(value);
    table.resetPagination(); // Reset to first page when filter changes
  };

  if (isLoading) {
    return (
      <PageSection>
        <LoadingEmptyState message={t('users.loading')} />
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
          {t('users.title')}
        </Title>
      </PageSection>
      <PageSection>
        <UsersTable
          kafkaId={kafkaId!}
          users={users}
          totalItems={totalItems}
          currentPage={currentPage}
          pageSize={table.pageSize}
          onPerPageSelect={table.handlePerPageChange}
          onNextClick={table.handleNextPage}
          onPreviousClick={table.handlePrevPage}
          sortBy={table.sortBy}
          sortDirection={table.sortDirection}
          onSort={table.handleSort}
          usernameFilter={usernameFilter}
          onUsernameFilterChange={handleUsernameFilterChange}
        />

        {users.length > 0 && (
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
                    paginationAriaLabel: t('users.tableLabel'),
                  }}
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
        )}
      </PageSection>
    </>
  );
}