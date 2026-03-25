import { apiClient, ApiResponse } from './client';

// Types
export type ConnectorState = 
  | 'UNASSIGNED'
  | 'RUNNING'
  | 'PAUSED'
  | 'STOPPED'
  | 'FAILED'
  | 'RESTARTING';

export type ConnectorType = 
  | 'source'
  | 'sink'
  | 'source:mm'
  | 'source:mm-checkpoint'
  | 'source:mm-heartbeat';

export interface Connector {
  id: string;
  type: 'connectors';
  attributes: {
    name: string;
    namespace?: string | null;
    creationTimestamp?: string | null;
    type: ConnectorType;
    state: ConnectorState;
    trace?: string | null;
    workerId?: string;
    topics?: string[];
  };
  relationships?: {
    connectCluster?: {
      data: {
        type: 'connects';
        id: string;
      } | null;
    };
  };
  meta?: {
    managed?: boolean;
  };
}

export interface ConnectCluster {
  id: string;
  type: 'connects';
  attributes: {
    name: string;
    namespace?: string | null;
    creationTimestamp?: string | null;
    commit?: string;
    kafkaClusterId?: string;
    version?: string;
    replicas?: number | null;
  };
  relationships?: {
    connectors?: {
      data: Array<{
        type: 'connectors';
        id: string;
      }>;
    };
  };
  meta?: {
    managed?: boolean;
  };
}

export interface ConnectorsResponse {
  data: Connector[];
  meta: {
    page: {
      total?: number;
      pageNumber?: number;
    };
  };
  links: {
    first: string | null;
    prev: string | null;
    next: string | null;
    last: string | null;
  };
  included?: ConnectCluster[];
}

export interface ConnectClustersResponse {
  data: ConnectCluster[];
  meta: {
    page: {
      total?: number;
      pageNumber?: number;
    };
  };
  links: {
    first: string | null;
    prev: string | null;
    next: string | null;
    last: string | null;
  };
  included?: Connector[];
}

export interface EnrichedConnector extends Connector {
  connectClusterId: string | null;
  connectClusterName: string | null;
  replicas: number | null;
}

// API Functions
export async function getConnectors(
  kafkaId: string,
  options: {
    page?: number;
    perPage?: number;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
  } = {}
): Promise<ApiResponse<ConnectorsResponse>> {
  const params = new URLSearchParams();
  
  // Filter by kafka cluster
  params.append('filter[connectCluster.kafkaClusters]', `in,${kafkaId}`);
  
  // Name filter
  if (options.name) {
    params.append('filter[name]', `like,${options.name}`);
  }
  
  // Pagination
  if (options.perPage) {
    params.append('page[size]', options.perPage.toString());
  }
  
  // Sorting
  if (options.sort) {
    const sortPrefix = options.sortDir === 'desc' ? '-' : '';
    params.append('sort', `${sortPrefix}${options.sort}`);
  }
  
  // Include related data
  params.append('include', 'connectCluster');
  params.append('fields[connectors]', 'name,type,state,connectCluster');
  
  return apiClient.get<ConnectorsResponse>(`/connectors?${params.toString()}`);
}

export async function getConnectClusters(
  kafkaId: string,
  options: {
    page?: number;
    perPage?: number;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
  } = {}
): Promise<ApiResponse<ConnectClustersResponse>> {
  const params = new URLSearchParams();
  
  // Filter by kafka cluster
  params.append('filter[kafkaClusters]', `in,${kafkaId}`);
  
  // Name filter
  if (options.name) {
    params.append('filter[name]', `like,${options.name}`);
  }
  
  // Pagination
  if (options.perPage) {
    params.append('page[size]', options.perPage.toString());
  }
  
  // Sorting
  if (options.sort) {
    const sortPrefix = options.sortDir === 'desc' ? '-' : '';
    params.append('sort', `${sortPrefix}${options.sort}`);
  }
  
  // Include related data
  params.append('include', 'connectors');
  params.append('fields[connects]', 'name,version,replicas,connectors');
  
  return apiClient.get<ConnectClustersResponse>(`/connects?${params.toString()}`);
}

// Helper function to enrich connectors with connect cluster info
export function enrichConnectors(
  connectors: Connector[],
  included?: ConnectCluster[]
): EnrichedConnector[] {
  const clusterMap = new Map((included ?? []).map(item => [item.id, item]));
  
  return connectors.map(connector => {
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

// Made with Bob