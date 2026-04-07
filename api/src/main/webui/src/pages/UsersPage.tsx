/**
 * Users Page - List all Kafka users in a cluster
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
} from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import { useUsers } from '../api/hooks/useUsers';
import { UsersTable } from '../components/UsersTable';

type SortableColumn = 'name' | 'namespace' | 'creationTimestamp' | 'username' | 'authenticationType';

export function UsersPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  const [pageSize, setPageSize] = useState(20);
  const [pageCursor, setPageCursor] = useState<string | undefined>(undefined);
  const [sortBy, setSortBy] = useState<SortableColumn>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [usernameFilter, setUsernameFilter] = useState('');

  const { data, isLoading, error } = useUsers(kafkaId, {
    pageSize,
    pageCursor,
    sort: sortBy,
    sortDir: sortDirection,
    username: usernameFilter || undefined,
  });

  const users = data?.data || [];
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

  const handleUsernameFilterChange = (value: string) => {
    setUsernameFilter(value);
    setPageCursor(undefined); // Reset to first page when filter changes
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
          <EmptyStateBody>{t('users.loading')}</EmptyStateBody>
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
            {error instanceof Error ? error.message : t('users.errorLoading')}
          </EmptyStateBody>
        </EmptyState>
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
          pageSize={pageSize}
          onPerPageSelect={handlePerPageChange}
          onNextClick={handleNextPage}
          onPreviousClick={handlePrevPage}
          sortBy={sortBy}
          sortDirection={sortDirection}
          onSort={handleSort}
          usernameFilter={usernameFilter}
          onUsernameFilterChange={handleUsernameFilterChange}
        />

        {users.length > 0 && (
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