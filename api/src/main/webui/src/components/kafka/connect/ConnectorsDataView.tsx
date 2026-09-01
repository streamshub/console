import React, { useCallback, useMemo } from 'react';
import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ThProps } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import { EnrichedConnector, ListResponse, ConnectorType } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
} from '@/components/common/ResourceListDataView';
import { ManagedConnectorLabel } from './ManagedConnectorLabel';
import { StatusLabel } from '@/components/StatusLabel';
import { CONNECTOR_STATE_CONFIG } from '@/components/StatusLabel/configs';

const columnNames = ['name', 'connectCluster'] as const;

interface ConnectorsDataViewProps {
  connectorsResult: UseQueryResult<ListResponse<EnrichedConnector>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
}

const TypeLabel: Record<ConnectorType, string> = {
  source: 'Source',
  sink: 'Sink',
  'source:mm': 'Mirror Source',
  'source:mm-checkpoint': 'Mirror Checkpoint',
  'source:mm-heartbeat': 'Mirror Heartbeat',
};

export function ConnectorsDataView({
  connectorsResult,
  onDataViewChange,
}: ConnectorsDataViewProps) {
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
        cell: t('kafka.connect.connectCluster', 'Connect cluster'),
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
        cell: t('kafka.connect.type', 'Type'),
      },
      {
        cell: t('kafka.connect.state', 'State'),
      },
      {
        cell: t('kafka.connect.tasks', 'Tasks'),
      },
    ],
    [t, handleSort]
  );

  const colProvider = useMemo(() => ({
    dependencies: [t, handleSort],
    callback: colMapper,
  }), [colMapper, t, handleSort]);

  const rowMapper: ResourceListDataViewRowMapper<EnrichedConnector> = useCallback(
    (connector) => ({
      id: connector.id,
      row: [
        {
          cell: (
            <>
              <Link to={`/kafka/${kafkaId}/connect/connectors/${encodeURIComponent(connector.id)}`}>
                {connector.attributes.name}
              </Link>
              {connector.meta?.managed === true && <ManagedConnectorLabel />}
            </>
          ),
          props: {
            dataLabel: t('kafka.connect.name', 'Name'),
          },
        },
        {
          cell: connector.connectClusterId ? (
            <Link to={`/kafka/${kafkaId}/connect/clusters/${connector.connectClusterId}`}>
              {connector.connectClusterName}
            </Link>
          ) : (
            '-'
          ),
          props: {
            dataLabel: t('kafka.connect.connectCluster', 'Connect cluster'),
          },
        },
        {
          cell: TypeLabel[connector.attributes.type],
          props: {
            dataLabel: t('kafka.connect.type', 'Type'),
          },
        },
        {
          cell: (
            <StatusLabel status={connector.attributes.state} config={CONNECTOR_STATE_CONFIG} />
          ),
          props: {
            dataLabel: t('kafka.connect.state', 'State'),
          },
        },
        {
          cell: connector.replicas ?? '-',
          props: {
            dataLabel: t('kafka.connect.tasks', 'Tasks'),
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
      resourceResult={connectorsResult}
      columnProvider={colProvider}
      rowProvider={rowProvider}
      onDataViewChange={handleDataViewChange}
      ariaLabel={t('kafka.connect.connectors', 'Connectors')}
      ouiaIdPrefix="kafka-connectors"
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
