/**
 * TanStack Query hooks for Nodes
 */

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../client';
import { CONFIG_DOCUMENTATION_FIELDS } from '../configFields';
import {
  NodeConfigResponse,
  Node,
  NodeListMeta,
} from '../types';
import { ResourceListParams, useResourceList } from './useResourceList';

/**
 * Fetch all nodes for a Kafka cluster.
 *
 * Filter keys (pass via params.filters):
 *   nodePool          – string array, matched with 'in'
 *   roles             – string array, matched with 'in'
 *   broker.status     – string array, matched with 'in'
 *   controller.status – string array, matched with 'in'
 */
export function useNodes(
  kafkaId: string | undefined,
  params?: ResourceListParams,
) {
  return useResourceList<Node, NodeListMeta>(
    'nodes',
    `/api/kafkas/${kafkaId}/nodes`,
    {
      ...params,
      enabled: !!kafkaId && (params?.enabled ?? true),
    },
  );
}

/**
 * Fetch a single node configuration
 */
export function useNodeConfig(
  kafkaId: string | undefined,
  nodeId: string | undefined,
  params?: {
    includeDocumentation?: boolean;
  }
) {
  return useQuery({
    queryKey: ['nodeConfig', kafkaId, nodeId, params?.includeDocumentation],
    queryFn: async () => {
      if (!kafkaId || !nodeId) {
        throw new Error('Kafka ID and Node ID are required');
      }

      const searchParams = new URLSearchParams();

      if (params?.includeDocumentation) {
        searchParams.set('fields[configs]', CONFIG_DOCUMENTATION_FIELDS);
      }

      const queryString = searchParams.toString();
      const path = `/api/kafkas/${kafkaId}/nodes/${nodeId}/configs${queryString ? `?${queryString}` : ''}`;

      return apiClient.get<NodeConfigResponse>(path);
    },
    enabled: !!kafkaId && !!nodeId,
  });
}