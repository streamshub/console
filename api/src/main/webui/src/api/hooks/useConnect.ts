/**
 * TanStack Query hooks for Kafka Connect
 */

import { useMemo } from 'react';
import { useQuery, UseQueryResult } from '@tanstack/react-query';
import { apiClient } from '../client';
import {
  ConnectorDetailResponse,
  ConnectClusterDetailResponse,
  EnrichedConnector,
  Connector,
  ConnectCluster,
  ListResponse,
  Resource,
} from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

/**
 * Enrich connectors with connect cluster information
 */
function enrichConnectorsData(
  connectors: Connector[],
  included?: Resource[]
): EnrichedConnector[] {
  const clusterMap = new Map((included ?? [])
    .filter((item) => item.type === 'connects')
    .map((item) => [item.id, item as ConnectCluster]));

  return connectors.map((connector) => {
    const connectClusterId = connector.relationships?.connectCluster?.data?.id;
    const cluster = connectClusterId ? clusterMap.get(connectClusterId) : null;

    return {
      ...connector,
      connectClusterId: connectClusterId ?? null,
      connectClusterName: cluster?.attributes?.name ?? null,
      replicas: cluster?.attributes?.replicas ?? null,
    };
  });
}

/**
 * Fetch all connectors for a Kafka cluster
 */
export function useConnectors(
  kafkaId: string | undefined,
  params?: ResourceListParams
) {
  const result = useResourceList<Connector>(
    'connectors',
    '/api/connectors',
    {
      ...params,
      include: 'connectCluster',
      fields: params?.fields ?? 'name,type,state,connectCluster',
      filters: {
        'connectCluster.kafkaClusters': {
          operator: 'in' as const,
          value: kafkaId || '',
        },
        ...params?.filters,
      },
      enabled: !!kafkaId && (params?.enabled ?? true),
    }
  );

  const enrichedData = useMemo(() => {
    if (!result.data) {
      return undefined;
    }
    const enriched = enrichConnectorsData(result.data.data ?? [], result.data.included);
    return {
      ...result.data,
      data: enriched,
    };
  }, [result.data]);

  return {
    ...result,
    data: enrichedData,
  } as unknown as UseQueryResult<ListResponse<EnrichedConnector>, Error>;
}

/**
 * Fetch all connect clusters for a Kafka cluster
 */
export function useConnectClusters(
  kafkaId: string | undefined,
  params?: ResourceListParams
) {
  return useResourceList<ConnectCluster>(
    'connects',
    '/api/connects',
    {
      ...params,
      include: 'connectors',
      fields: params?.fields ?? 'name,version,replicas,connectors',
      filters: {
        'kafkaClusters': {
          operator: 'in' as const,
          value: kafkaId || '',
        },
        ...params?.filters,
      },
      enabled: !!kafkaId && (params?.enabled ?? true),
    }
  );
}

/**
 * Fetch a single connector by ID
 */
export function useConnector(connectorId: string | undefined) {
  return useQuery({
    queryKey: ['connector', connectorId],
    queryFn: async () => {
      if (!connectorId) {
        throw new Error('Connector ID is required');
      }

      const searchParams = new URLSearchParams();
      searchParams.set('include', 'connectCluster,tasks');
      searchParams.set('fields[connectorTasks]', 'taskId,state,workerId,config');
      searchParams.set(
        'fields[connectors]',
        'name,state,type,connectCluster,topics,config,tasks,workerId'
      );

      const path = `/api/connectors/${connectorId}?${searchParams}`;

      return apiClient.get<ConnectorDetailResponse>(path);
    },
    enabled: !!connectorId,
  });
}

/**
 * Fetch a single connect cluster by ID
 */
export function useConnectCluster(connectClusterId: string | undefined) {
  return useQuery({
    queryKey: ['connectCluster', connectClusterId],
    queryFn: async () => {
      if (!connectClusterId) {
        throw new Error('Connect Cluster ID is required');
      }

      const searchParams = new URLSearchParams();
      searchParams.set('include', 'connectors');
      searchParams.set('fields[connects]', 'name,version,replicas,connectors,plugins');

      const path = `/api/connects/${connectClusterId}?${searchParams}`;

      return apiClient.get<ConnectClusterDetailResponse>(path);
    },
    enabled: !!connectClusterId,
  });
}