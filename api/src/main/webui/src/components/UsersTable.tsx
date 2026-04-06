/**
 * Kafka Users Table Component
 * Displays a table of Kafka users with filtering and sorting
 */

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  TextInput,
  InputGroup,
  EmptyState,
  Title,
  EmptyStateBody,
} from '@patternfly/react-core';
import { CubesIcon, SearchIcon } from '@patternfly/react-icons';
import { KafkaUser } from '../api/types';
import { formatDateTime } from '../utils/dateTime';

type SortableColumn = 'name' | 'namespace' | 'creationTimestamp' | 'username' | 'authenticationType';

interface UsersTableProps {
  kafkaId: string;
  users: KafkaUser[];
  sortBy: SortableColumn;
  sortDirection: 'asc' | 'desc';
  onSort: (column: SortableColumn) => void;
  usernameFilter: string;
  onUsernameFilterChange: (value: string) => void;
}

export function UsersTable({
  kafkaId,
  users,
  sortBy,
  sortDirection,
  onSort,
  usernameFilter,
  onUsernameFilterChange,
}: UsersTableProps) {
  const { t } = useTranslation();

  const getSortParams = (column: SortableColumn): ThProps['sort'] => {
    const columnIndex = ['name', 'namespace', 'creationTimestamp', 'username', 'authenticationType'].indexOf(column);
    return {
      sortBy: {
        index: columnIndex,
        direction: sortBy === column ? sortDirection : undefined,
        defaultDirection: 'asc',
      },
      onSort: () => onSort(column),
      columnIndex,
    };
  };

  if (users.length === 0 && !usernameFilter) {
    return (
      <EmptyState>
        <CubesIcon />
        <Title headingLevel="h2" size="lg">
          {t('users.noUsers')}
        </Title>
        <EmptyStateBody>{t('users.noUsersDescription')}</EmptyStateBody>
      </EmptyState>
    );
  }

  if (users.length === 0 && usernameFilter) {
    return (
      <EmptyState>
        <SearchIcon />
        <Title headingLevel="h2" size="lg">
          {t('common.noResultsFound')}
        </Title>
        <EmptyStateBody>{t('common.noResultsFoundDescription')}</EmptyStateBody>
      </EmptyState>
    );
  }

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <InputGroup>
              <TextInput
                type="text"
                id="username-filter"
                placeholder={t('users.filter.usernamePlaceholder')}
                value={usernameFilter}
                onChange={(_event, value) => onUsernameFilterChange(value)}
                aria-label={t('users.filter.usernameLabel')}
              />
            </InputGroup>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      <Table aria-label={t('users.tableLabel')} variant="compact">
        <Thead>
          <Tr>
            <Th sort={getSortParams('name')}>{t('users.columnName')}</Th>
            <Th sort={getSortParams('namespace')}>{t('users.columnNamespace')}</Th>
            <Th sort={getSortParams('creationTimestamp')}>{t('users.columnCreationTime')}</Th>
            <Th sort={getSortParams('username')}>{t('users.columnUsername')}</Th>
            <Th sort={getSortParams('authenticationType')}>{t('users.columnAuthentication')}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {users.map((user) => {
            const canViewDetails = user.meta?.privileges?.includes('GET') === true;

            return (
              <Tr key={user.id}>
                <Td dataLabel={t('users.columnName')}>
                  {canViewDetails ? (
                    <Link to={`/kafka/${kafkaId}/users/${user.id}`}>
                      {user.attributes.name}
                    </Link>
                  ) : (
                    user.attributes.name
                  )}
                </Td>
                <Td dataLabel={t('users.columnNamespace')}>
                  {user.attributes.namespace ?? '-'}
                </Td>
                <Td dataLabel={t('users.columnCreationTime')}>
                  {user.attributes.creationTimestamp
                    ? formatDateTime({ value: user.attributes.creationTimestamp })
                    : '-'}
                </Td>
                <Td dataLabel={t('users.columnUsername')}>
                  {user.attributes.username}
                </Td>
                <Td dataLabel={t('users.columnAuthentication')}>
                  {user.attributes.authenticationType}
                </Td>
              </Tr>
            );
          })}
        </Tbody>
      </Table>
    </>
  );
}