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
import { UseQueryResult } from '@tanstack/react-query';
import { KafkaAuthShowLoginModalType, useKafkaAuthContext } from '@/components/auth/KafkaAuthProvider';
import { apiClient, ApiError } from '@/api/client';

const columnNames = ['name', 'namespace', 'version', 'status'];

interface ClustersDataViewProps {
  clusterResult: UseQueryResult<ListResponse<KafkaCluster>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
}

async function handleViewCluster(cluster: KafkaCluster, showLoginModal: KafkaAuthShowLoginModalType, navigate: ReturnType<typeof useNavigate>) {
  // Check if cluster requires authentication (basic or oauth)
    const requiresAuth = cluster.meta?.authentication?.method === 'basic' ||
                         cluster.meta?.authentication?.method === 'oauth';

    if (requiresAuth) {
      // Try to access the cluster to check if authentication is needed
      try {
        await apiClient.get(`/api/kafkas/${cluster.id}`);
        // If successful, navigate to the cluster
        navigate(`/kafka/${cluster.id}`);
      } catch (error) {
        // If 401, show login modal
        if (error instanceof ApiError && error.status === 401) {
          showLoginModal(
            cluster.id,
            cluster.attributes.name,
            cluster.meta?.authentication?.method,
            `/kafka/${cluster.id}`
          );
        } else {
          // For other errors, just navigate (let the cluster page handle it)
          navigate(`/kafka/${cluster.id}`);
        }
      }
    } else {
      // No authentication required, navigate directly
      navigate(`/kafka/${cluster.id}`);
    }
}

export function ClustersDataView({
  clusterResult,
  onDataViewChange,
}: ClustersDataViewProps) {

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showLoginModal } = useKafkaAuthContext();

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
              onClick={() => handleViewCluster(cluster, showLoginModal, navigate)}
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
    [t, showLoginModal, navigate]
  );

  return (
    <ResourceListDataView
      resourceResult={clusterResult}
      onDataViewChange={onDataViewChange}
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
