import React, { useCallback, useMemo } from 'react';
import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ThProps } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import { ConnectCluster, ListResponse } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
} from '@/components/common/ResourceListDataView';
import { HelpIcon } from '@patternfly/react-icons';
import { Tooltip } from '@patternfly/react-core';

const columnNames = ['name', 'version'] as const;

interface ConnectClustersDataViewProps {
  connectClustersResult: UseQueryResult<ListResponse<ConnectCluster>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
}

export function ConnectClustersDataView({
  connectClustersResult,
  onDataViewChange,
}: ConnectClustersDataViewProps) {
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
        cell: t('kafka.connect.name', 'Name'),
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
        cell: t('kafka.connect.version', 'Version'),
        props: {
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 1,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
      {
        cell: (
          <>
            {t('kafka.connect.workers', 'Workers')}{' '}
            <Tooltip content={t('kafka.connect.workersTooltip', 'Number of worker nodes')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
      },
    ],
    [t, handleSort]
  );

  const colProvider = useMemo(() => ({
    dependencies: [t, handleSort],
    callback: colMapper,
  }), [colMapper, t, handleSort]);

  const rowMapper: ResourceListDataViewRowMapper<ConnectCluster> = useCallback(
    (cluster) => ({
      id: cluster.id,
      row: [
        {
          cell: (
            <Link to={`/kafka/${kafkaId}/connect/clusters/${encodeURIComponent(cluster.id)}`}>
              {cluster.attributes.name}
            </Link>
          ),
          props: {
            dataLabel: t('kafka.connect.name', 'Name'),
          },
        },
        {
          cell: cluster.attributes.version || '-',
          props: {
            dataLabel: t('kafka.connect.version', 'Version'),
          },
        },
        {
          cell: cluster.attributes.replicas ?? '-',
          props: {
            dataLabel: t('kafka.connect.workers', 'Workers'),
          },
        },
      ],
    }),
    [kafkaId, t]
  );

  const rowProvider = useMemo(() => ({
    dependencies: [kafkaId, t],
    callback: rowMapper,
  }), [rowMapper, kafkaId, t]);

  return (
    <ResourceListDataView
      resourceResult={connectClustersResult}
      columnProvider={colProvider}
      rowProvider={rowProvider}
      onDataViewChange={handleDataViewChange}
      ariaLabel={t('kafka.connect.connectClusters', 'Connect Clusters')}
      ouiaIdPrefix="kafka-connect-clusters"
      dataFilters={{
        name: {
          type: 'text',
          title: t('common.name'),
          placeholder: t('common.filterByName'),
        },
      }}
    />
  );
}
