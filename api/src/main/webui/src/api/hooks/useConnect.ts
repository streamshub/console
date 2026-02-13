/**
 * TanStack Query hooks for Kafka Connect
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import {
  ConnectorsResponse,
  ConnectClustersResponse,
  ConnectorDetailResponse,
  ConnectClusterDetailResponse,
  EnrichedConnector,
} from '../types';

/**
 * Enrich connectors with connect cluster information
 */
function enrichConnectorsData(
  connectors: ConnectorsResponse['data'],
  included: ConnectorsResponse['included']
): EnrichedConnector[] {
  const clusterMap = new Map((included ?? []).map((item) => [item.id, item]));

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
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
  }
) {
  return useQuery({
    queryKey: [
      'connectors',
      kafkaId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.name,
    ],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('Kafka ID is required');
      }

      const searchParams = new URLSearchParams();

      searchParams.set('filter[connectCluster.kafkaClusters]', `in,${kafkaId}`);

      if (params?.name) {
        searchParams.set('filter[name]', `like,*${params.name}*`);
      }

      if (params?.pageSize) {
        searchParams.set('page[size]', params.pageSize.toString());
      }

      // Handle cursor-based pagination
      if (params?.pageCursor) {
        if (params.pageCursor.startsWith('after:')) {
          searchParams.set('page[after]', params.pageCursor.slice(6));
        } else if (params.pageCursor.startsWith('before:')) {
          searchParams.set('page[before]', params.pageCursor.slice(7));
        }
      }

      if (params?.sort) {
        const sortPrefix = params.sortDir === 'desc' ? '-' : '';
        searchParams.set('sort', `${sortPrefix}${params.sort}`);
      }

      searchParams.set('include', 'connectCluster');
      searchParams.set('fields[connectors]', 'name,type,state,connectCluster');

      const path = `/api/connectors?${searchParams}`;

      const response = await apiClient.get<ConnectorsResponse>(path);

      // Enrich the data with connect cluster information
      const enrichedData = enrichConnectorsData(response.data, response.included);

      return {
        ...response,
        data: enrichedData,
      };
    },
    enabled: !!kafkaId,
  });
}

/**
 * Fetch all connect clusters for a Kafka cluster
 */
export function useConnectClusters(
  kafkaId: string | undefined,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
  }
) {
  return useQuery({
    queryKey: [
      'connectClusters',
      kafkaId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.name,
    ],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('Kafka ID is required');
      }

      const searchParams = new URLSearchParams();

      searchParams.set('filter[kafkaClusters]', `in,${kafkaId}`);

      if (params?.name) {
        searchParams.set('filter[name]', `like,*${params.name}*`);
      }

      if (params?.pageSize) {
        searchParams.set('page[size]', params.pageSize.toString());
      }

      // Handle cursor-based pagination
      if (params?.pageCursor) {
        if (params.pageCursor.startsWith('after:')) {
          searchParams.set('page[after]', params.pageCursor.slice(6));
        } else if (params.pageCursor.startsWith('before:')) {
          searchParams.set('page[before]', params.pageCursor.slice(7));
        }
      }

      if (params?.sort) {
        const sortPrefix = params.sortDir === 'desc' ? '-' : '';
        searchParams.set('sort', `${sortPrefix}${params.sort}`);
      }

      searchParams.set('include', 'connectors');
      searchParams.set('fields[connects]', 'name,version,replicas,connectors');

      const path = `/api/connects?${searchParams}`;

      return apiClient.get<ConnectClustersResponse>(path);
    },
    enabled: !!kafkaId,
  });
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