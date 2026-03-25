import { apiClient } from './client';

export type RebalanceStatus = 
  | 'New'
  | 'PendingProposal'
  | 'ProposalReady'
  | 'Rebalancing'
  | 'Stopped'
  | 'NotReady'
  | 'Ready'
  | 'ReconciliationPaused';

export type RebalanceMode = 'full' | 'add-brokers' | 'remove-brokers';

export interface OptimizationResult {
  numIntraBrokerReplicaMovements?: number;
  numReplicaMovements?: number;
  onDemandBalancednessScoreAfter?: number;
  afterBeforeLoadConfigMap?: string;
  intraBrokerDataToMoveMB?: number;
  monitoredPartitionsPercentage?: number;
  provisionRecommendation?: string;
  excludedBrokersForReplicaMove?: string[] | null;
  excludedBrokersForLeadership?: string[] | null;
  provisionStatus?: string;
  onDemandBalancednessScoreBefore?: number;
  recentWindows?: number;
  dataToMoveMB?: number;
  excludedTopics?: string[] | null;
  numLeaderMovements?: number;
}

export interface Rebalance {
  id: string;
  type: 'kafkaRebalances';
  meta: {
    page: {
      cursor: string;
    };
    autoApproval: boolean;
    managed?: boolean;
    allowedActions: string[];
    privileges?: string[];
  };
  attributes: {
    name: string;
    status: RebalanceStatus | null;
    creationTimestamp: string;
    mode: RebalanceMode;
    brokers: number[] | null;
    optimizationResult: OptimizationResult;
    conditions?: Array<{
      type?: string;
      status?: string;
      reason?: string;
      message?: string;
      lastTransitionTime?: string;
    }> | null;
  };
}

export interface RebalancesResponse {
  data: Rebalance[];
  meta: {
    page: {
      total: number;
      pageNumber?: number;
    };
  };
  links: {
    first: string | null;
    prev: string | null;
    next: string | null;
    last: string | null;
  };
}

export interface GetRebalancesParams {
  name?: string;
  mode?: RebalanceMode[];
  status?: RebalanceStatus[];
  page?: number;
  perPage?: number;
  sort?: string;
  sortDir?: 'asc' | 'desc';
}

export async function getRebalances(
  kafkaId: string,
  params: GetRebalancesParams = {}
): Promise<RebalancesResponse> {
  const searchParams = new URLSearchParams();
  
  searchParams.append('fields[kafkaRebalances]', 
    'name,namespace,creationTimestamp,status,mode,brokers,optimizationResult,conditions');
  
  if (params.name) {
    searchParams.append('filter[name]', `like,*${params.name}*`);
  }
  
  if (params.status && params.status.length > 0) {
    searchParams.append('filter[status]', `in,${params.status.join(',')}`);
  }
  
  if (params.mode && params.mode.length > 0) {
    searchParams.append('filter[mode]', `in,${params.mode.join(',')}`);
  }
  
  if (params.perPage) {
    searchParams.append('page[size]', params.perPage.toString());
  }
  
  if (params.sort && params.sortDir) {
    const sortPrefix = params.sortDir === 'desc' ? '-' : '';
    searchParams.append('sort', `${sortPrefix}${params.sort}`);
  }

  const response = await apiClient.get<RebalancesResponse>(
    `/kafkas/${kafkaId}/rebalances?${searchParams.toString()}`
  );

  if (response.errors) {
    throw new Error(response.errors[0]?.title || 'Failed to fetch rebalances');
  }

  return response.data!;
}

// Made with Bob