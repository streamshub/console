import React, { useCallback, useMemo } from 'react';
import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ThProps } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import { KafkaUser, ListResponse } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
} from '@/components/common/ResourceListDataView';
import { formatDateTime } from '@/utils/dateTime';

const columnNames = ['name', 'namespace', 'creationTimestamp', 'username', 'authenticationType'] as const;

interface UsersDataViewProps {
  usersResult: UseQueryResult<ListResponse<KafkaUser>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
}

export function UsersDataView({
  usersResult,
  onDataViewChange,
}: UsersDataViewProps) {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    const filters: Record<string, string | string[]> = {};

    Object.entries(params.filters ?? {}).forEach(([key, value]) => {
      if (typeof value === 'string' && value.length > 0) {
        filters[key] = value;
      } else if (Array.isArray(value) && value.length > 0) {
        filters[key] = value;
      }
    });

    onDataViewChange({
      ...params,
      filters,
    });
  }, [onDataViewChange]);

  const handleSort = useCallback((
    onSort: ((event: React.MouseEvent, sortBy: string, direction: 'asc' | 'desc') => void) | undefined,
    event: React.MouseEvent,
    columnIndex: number,
    direction: 'asc' | 'desc'
  ) => {
    onSort?.(event, columnNames[columnIndex], direction);
  }, []);

  const colMapper: ResourceListDataViewColumnMapper = useCallback(
    (sortBy, direction, onSort) => [
      {
        cell: t('users.columnName'),
        props: {
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 0,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
      {
        cell: t('users.columnNamespace'),
      },
      {
        cell: t('users.columnCreationTime'),
      },
      {
        cell: t('users.columnUsername'),
      },
      {
        cell: t('users.columnAuthentication'),
      },
    ],
    [t, handleSort]
  );

  const colProvider = useMemo(() => ({
    dependencies: [t, handleSort],
    callback: colMapper,
  }), [colMapper, t, handleSort]);

  const rowMapper: ResourceListDataViewRowMapper<KafkaUser> = useCallback(
    (user) => {
      const canViewDetails = user.meta?.privileges?.includes('GET') === true;

      return {
        id: user.id,
        row: [
          {
            cell: canViewDetails ? (
              <Link to={`/kafka/${kafkaId}/users/${user.id}`}>
                {user.attributes.name}
              </Link>
            ) : (
              user.attributes.name
            ),
            props: {
              dataLabel: t('users.columnName'),
            },
          },
          {
            cell: user.attributes.namespace ?? '-',
            props: {
              dataLabel: t('users.columnNamespace'),
            },
          },
          {
            cell: user.attributes.creationTimestamp
              ? formatDateTime({ value: user.attributes.creationTimestamp })
              : '-',
            props: {
              dataLabel: t('users.columnCreationTime'),
            },
          },
          {
            cell: user.attributes.username,
            props: {
              dataLabel: t('users.columnUsername'),
            },
          },
          {
            cell: user.attributes.authenticationType,
            props: {
              dataLabel: t('users.columnAuthentication'),
            },
          },
        ],
      };
    },
    [kafkaId, t]
  );

  const rowProvider = useMemo(() => ({
    dependencies: [kafkaId, t],
    callback: rowMapper,
  }), [rowMapper, kafkaId, t]);

  return (
    <ResourceListDataView
      resourceResult={usersResult}
      columnProvider={colProvider}
      rowProvider={rowProvider}
      onDataViewChange={handleDataViewChange}
      ariaLabel={t('users.tableLabel')}
      ouiaIdPrefix="kafka-users"
      dataFilters={{
        username: {
          type: 'text',
          title: t('users.filter.usernameLabel'),
          placeholder: t('users.filter.usernamePlaceholder'),
        },
      }}
    />
  );
}
