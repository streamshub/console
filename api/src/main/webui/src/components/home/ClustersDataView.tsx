import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Button,
  Truncate,
} from '@patternfly/react-core';
import { ThProps } from '@patternfly/react-table';
import { KafkaCluster, ListResponse } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper
} from '../common/ResourceListDataView';

const columnNames = ['name', 'namespace', 'version', 'status'];

interface ClustersDataViewProps {
  clusterResponse?: ListResponse<KafkaCluster>;
  isLoading?: boolean;
  onDataViewChange: (params: ResourceListParams) => void;
}

export function ClustersDataView({
  clusterResponse,
  isLoading = false,
  onDataViewChange,
}: ClustersDataViewProps) {

  const { t } = useTranslation();
  const navigate = useNavigate();

  // Memoized sort handler to avoid recreating on every render
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
        cell: t('kafka.name'),
        props: {
          width: 25,
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy) : undefined,
              direction: direction,
            },
            columnIndex: 0,
            onSort: (event, columnIndex, direction) =>
              handleSort(onSort, event, columnIndex, direction),
          } as ThProps['sort'],
        },
      },
      {
        cell: t('kafka.namespace'),
      },
      {
        cell: t('kafka.version'),
      },
      {
        cell: t('kafka.status'),
      },
      {
        cell: t('common.actions'),
        props: {
          modifier: 'fitContent',
        },
      },
    ],
    [t, handleSort]
  );

  const rowMapper: ResourceListDataViewRowMapper<KafkaCluster> = useCallback(
    (cluster) => ({
      id: cluster.id,
      row: [
        {
          cell: <Truncate content={cluster.attributes.name} />,
          props: {
            dataLabel: t('kafka.name'),
          },
        },
        {
          cell: cluster.attributes.namespace || t('common.notAvailable', 'N/A'),
          props: {
            dataLabel: t('kafka.namespace'),
          },
        },
        {
          cell: cluster.attributes.kafkaVersion || t('common.notAvailable', 'N/A'),
          props: {
            dataLabel: t('kafka.version'),
          },
        },
        {
          cell: cluster.attributes.status || t('common.notAvailable', 'N/A'),
          props: {
            dataLabel: t('kafka.status'),
          },
        },
        {
          cell: (
            <Button
              variant="primary"
              onClick={() => navigate(`/kafka/${cluster.id}`)}
            >
              {t('common.view')}
            </Button>
          ),
          props: {
            dataLabel: t('common.actions'),
            modifier: 'fitContent',
          },
        },
      ],
    }),
    [t, navigate]
  );

  return (
    <ResourceListDataView
      listResponse={clusterResponse}
      onDataViewChange={onDataViewChange}
      isLoading={isLoading}
      ariaLabel={t('kafka.clusterList')}
      ouiaIdPrefix='kafka-clusters'
      dataFilters={{
        name: {
          type: 'text',
          title: t('kafka.name'),
          placeholder: t('kafka.filterByName'),
        },
      }}
      columnProvider={{
        dependencies: [t, handleSort],
        callback: colMapper,
      }}
      rowProvider={{
        dependencies: [navigate, t],
        callback: rowMapper,
      }}
      />
  );
}
