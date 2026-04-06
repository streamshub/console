/**
 * TanStack Query hooks for Nodes
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../client';
import {
  NodesResponse,
  NodeConfigResponse,
  BrokerStatus,
  ControllerStatus,
  NodeRoles,
} from '../types';

/**
 * Fetch all nodes for a Kafka cluster
 */
export function useNodes(
  kafkaId: string | undefined,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    nodePool?: string[];
    roles?: NodeRoles[];
    brokerStatus?: BrokerStatus[];
    controllerStatus?: ControllerStatus[];
    fields?: string[];
  }
) {
  return useQuery({
    queryKey: [
      'nodes',
      kafkaId,
      params?.pageSize,
      params?.pageCursor,
      params?.sort,
      params?.sortDir,
      params?.nodePool,
      params?.roles,
      params?.brokerStatus,
      params?.controllerStatus,
      params?.fields,
    ],
    queryFn: async () => {
      if (!kafkaId) {
        throw new Error('Kafka ID is required');
      }

      const searchParams = new URLSearchParams();

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

      if (params?.nodePool && params.nodePool.length > 0) {
        searchParams.set('filter[nodePool]', `in,${params.nodePool.join(',')}`);
      }

      if (params?.roles && params.roles.length > 0) {
        searchParams.set('filter[roles]', `in,${params.roles.join(',')}`);
      }

      if (params?.brokerStatus && params.brokerStatus.length > 0) {
        searchParams.set('filter[broker.status]', `in,${params.brokerStatus.join(',')}`);
      }

      if (params?.controllerStatus && params.controllerStatus.length > 0) {
        searchParams.set('filter[controller.status]', `in,${params.controllerStatus.join(',')}`);
      }

      if (params?.fields) {
        searchParams.set('fields[nodes]', params.fields.join(','));
      }

      const queryString = searchParams.toString();
      const path = `/api/kafkas/${kafkaId}/nodes${queryString ? `?${queryString}` : ''}`;

      return apiClient.get<NodesResponse>(path);
    },
    enabled: !!kafkaId,
  });
}

/**
 * Fetch a single node configuration
 */
export function useNodeConfig(
  kafkaId: string | undefined,
  nodeId: string | undefined
) {
  return useQuery({
    queryKey: ['nodeConfig', kafkaId, nodeId],
    queryFn: async () => {
      if (!kafkaId || !nodeId) {
        throw new Error('Kafka ID and Node ID are required');
      }

      const path = `/api/kafkas/${kafkaId}/nodes/${nodeId}/configs`;

      return apiClient.get<NodeConfigResponse>(path);
    },
    enabled: !!kafkaId && !!nodeId,
  });
}