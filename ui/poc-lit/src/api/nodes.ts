import { apiClient, ApiResponse } from './client';

export type NodeRole = 'broker' | 'controller';
export type BrokerStatus = 'NotRunning' | 'Starting' | 'Recovery' | 'Running' | 'PendingControlledShutdown' | 'ShuttingDown' | 'Unknown';
export type ControllerStatus = 'QuorumLeader' | 'QuorumFollower' | 'QuorumFollowerLagged' | 'Unknown';

export interface Node {
  id: string;
  type: 'nodes';
  attributes: {
    host?: string | null;
    port?: number | null;
    rack?: string | null;
    nodePool?: string | null;
    kafkaVersion?: string | null;
    roles?: NodeRole[] | null;
    metadataState?: {
      status: 'leader' | 'follower' | 'observer';
      logEndOffset: number;
      lag: number;
    } | null;
    broker?: {
      status: BrokerStatus;
      replicaCount: number;
      leaderCount: number;
    } | null;
    controller?: {
      status: ControllerStatus;
    } | null;
    storageUsed?: number | null;
    storageCapacity?: number | null;
  };
}

export interface NodesResponse {
  data: Node[];
  meta: {
    page: {
      total: number;
      pageNumber?: number;
    };
    summary: {
      nodePools: Record<string, {
        roles: string[];
        count: number;
      }>;
      statuses: {
        brokers?: Record<string, number>;
        controllers?: Record<string, number>;
        combined?: Record<string, number>;
      };
      leaderId?: string;
    };
  };
  links: {
    first: string | null;
    prev: string | null;
    next: string | null;
    last: string | null;
  };
}

export interface GetNodesParams {
  roles?: NodeRole[];
  brokerStatus?: BrokerStatus[];
  controllerStatus?: ControllerStatus[];
  nodePool?: string[];
  sort?: string;
  sortDir?: 'asc' | 'desc';
  pageSize?: number;
  pageCursor?: string;
}

export async function getNodes(
  kafkaId: string,
  params: GetNodesParams = {}
): Promise<ApiResponse<NodesResponse>> {
  const searchParams = new URLSearchParams();
  
  if (params.roles?.length) searchParams.set('filter[roles][in]', params.roles.join(','));
  if (params.brokerStatus?.length) searchParams.set('filter[broker.status][in]', params.brokerStatus.join(','));
  if (params.controllerStatus?.length) searchParams.set('filter[controller.status][in]', params.controllerStatus.join(','));
  if (params.nodePool?.length) searchParams.set('filter[nodePool][in]', params.nodePool.join(','));
  if (params.sort) searchParams.set('sort', `${params.sortDir === 'desc' ? '-' : ''}${params.sort}`);
  if (params.pageSize) searchParams.set('page[size]', params.pageSize.toString());
  
  if (params.pageCursor) {
    const [direction, cursor] = params.pageCursor.split(':');
    if (direction === 'after') {
      searchParams.set('page[after]', cursor);
    } else if (direction === 'before') {
      searchParams.set('page[before]', cursor);
    }
  }

  return apiClient.get<NodesResponse>(`/kafkas/${kafkaId}/nodes?${searchParams}`);
}

// Made with Bob
