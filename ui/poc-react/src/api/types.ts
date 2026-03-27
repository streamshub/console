/**
 * API Type Definitions
 * 
 * These types match the API responses from the Quarkus backend.
 * Copied and adapted from the existing Next.js implementation.
 */

// Common types
export interface ApiResponse<T> {
  data?: T;
  errors?: ApiError[];
  meta?: Record<string, unknown>;
  links?: {
    first?: string;
    last?: string;
    prev?: string;
    next?: string;
  };
}

export interface ApiError {
  status: string;
  title: string;
  detail?: string;
}

export interface ResourceIdentifier {
  type: string;
  id: string;
}

export interface Resource<T = Record<string, unknown>> {
  type: string;
  id: string;
  attributes?: T;
  relationships?: Record<string, { data: ResourceIdentifier | ResourceIdentifier[] }>;
  meta?: Record<string, unknown>;
}

// Kafka Cluster types
export interface KafkaCluster {
  id: string;
  type: 'kafkas';
  attributes: {
    name: string;
    namespace?: string;
    status?: string;
    kafkaVersion?: string;
    creationTimestamp?: string;
    listeners?: Array<{
      name: string;
      bootstrapServers: string;
    }>;
  };
  meta?: {
    authentication?: {
      method: string;
    };
    kind?: string;
  };
}

export interface KafkaClustersResponse {
  data: KafkaCluster[];
  meta?: {
    page?: {
      total?: number;
    };
  };
}

// Topic types
export interface Partition {
  partition: number;
  status: 'FullyReplicated' | 'UnderReplicated' | 'Offline';
  leaderId?: number;
  replicas: Array<{
    nodeId: number;
    nodeRack?: string;
    inSync: boolean;
    localStorage?: {
      size: number;
      offsetLag: number;
      future: boolean;
    };
  }>;
  offsets?: {
    earliest?: { offset?: number; timestamp?: string; leaderEpoch?: number };
    latest?: { offset?: number; timestamp?: string; leaderEpoch?: number };
    maxTimestamp?: { offset?: number; timestamp?: string; leaderEpoch?: number };
    timestamp?: { offset?: number; timestamp?: string; leaderEpoch?: number };
  } | null;
  leaderLocalStorage?: number;
}

export interface ConfigValue {
  value?: string;
  source: string;
  sensitive: boolean;
  readOnly: boolean;
  type: string;
  documentation?: string;
}

export interface Topic {
  id: string;
  type: 'topics';
  meta?: {
    managed?: boolean;
    privileges?: string[];
  };
  attributes: {
    name: string;
    status?: 'FullyReplicated' | 'UnderReplicated' | 'PartiallyOffline' | 'Offline' | 'Unknown';
    visibility?: 'internal' | 'external';
    partitions?: Partition[];
    numPartitions?: number | null;
    authorizedOperations?: string[];
    configs?: Record<string, ConfigValue>;
    totalLeaderLogBytes?: number | null;
  };
  relationships?: {
    groups?: {
      meta?: {
        count?: number;
      };
      data?: unknown[];
    } | null;
  };
}

export interface TopicsResponse {
  data: Topic[];
  meta: {
    page: {
      total: number;
      pageNumber: number;
    };
  };
  links: {
    next?: string;
    prev?: string;
  };
}

// Group types
export type GroupState =
  | 'UNKNOWN'
  | 'PREPARING_REBALANCE'
  | 'COMPLETING_REBALANCE'
  | 'STABLE'
  | 'DEAD'
  | 'EMPTY'
  | 'ASSIGNING'
  | 'RECONCILING';

export type GroupType = 'Classic' | 'Consumer' | 'Share' | 'Streams';

export interface PartitionKey {
  topicId?: string;
  topicName: string;
  partition: number;
}

export interface OffsetAndMetadata {
  topicId?: string;
  topicName: string;
  partition: number;
  offset: number | null;
  logEndOffset?: number;
  lag?: number;
  metadata?: string;
  leaderEpoch?: number;
}

export interface MemberDescription {
  memberId: string;
  groupInstanceId?: string | null;
  clientId: string;
  host: string;
  assignments?: PartitionKey[];
}

export interface Group {
  id: string;
  type: 'groups';
  meta?: {
    privileges?: string[];
    describeAvailable?: boolean;
  };
  attributes: {
    groupId: string;
    type?: GroupType | null;
    protocol?: string | null;
    simpleConsumerGroup?: boolean;
    state: GroupState;
    members?: MemberDescription[] | null;
    partitionAssignor?: string | null;
    coordinator?: {
      id: string;
      type: 'nodes';
      attributes: {
        nodeId: number;
        host?: string;
        port?: number;
        rack?: string;
      };
    } | null;
    authorizedOperations?: string[] | null;
    offsets?: OffsetAndMetadata[] | null;
    configs?: Record<string, ConfigValue>;
  };
}

export interface GroupsResponse {
  data: Group[];
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
}

// Node types
// Node types
export type NodeRoles = 'broker' | 'controller';

export type BrokerStatus =
  | 'NotRunning'
  | 'Starting'
  | 'Recovery'
  | 'Running'
  | 'PendingControlledShutdown'
  | 'ShuttingDown'
  | 'Unknown';

export type ControllerStatus =
  | 'QuorumLeader'
  | 'QuorumFollower'
  | 'QuorumFollowerLagged'
  | 'Unknown';

export interface NodePoolMeta {
  roles: string[];
  count: number;
}

export type NodePools = Record<string, NodePoolMeta>;

export type Statuses = Record<
  'brokers' | 'controllers' | 'combined',
  Record<string, number>
>;

export interface Node {
  id: string;
  type: 'nodes';
  attributes: {
    host?: string | null;
    port?: number | null;
    rack?: string | null;
    nodePool?: string | null;
    kafkaVersion?: string | null;
    roles?: NodeRoles[] | null;
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
    summary: {
      nodePools: NodePools;
      statuses: Statuses;
      leaderId?: string;
    };
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

export interface NodeConfigResponse {
  data: {
    id?: string;
    type: string;
    meta?: Record<string, unknown>;
    attributes: Record<string, ConfigValue>;
  };
}

// Metadata types
export interface Metadata {
  id: string;
  type: 'metadata';
  attributes: {
    version: string;
    platform: string;
  };
}

export interface MetadataResponse {
  data: Metadata;
}

// Kafka Record (Message) types
export interface RelatedSchema {
  meta?: {
    artifactType?: string;
    name?: string;
    errors?: ApiError[];
  } | null;
  links?: {
    content: string;
  } | null;
  data?: {
    type: 'schemas';
    id: string;
  };
}

export interface KafkaRecord {
  type: 'records';
  attributes: {
    partition: number;
    offset: number;
    timestamp: string;
    timestampType: string;
    headers: Record<string, unknown>;
    key: string | null;
    value: string | null;
    size?: number;
  };
  relationships: {
    keySchema?: RelatedSchema | null;
    valueSchema?: RelatedSchema | null;
  };
}

export interface KafkaRecordsResponse {
  data: KafkaRecord[];
  meta?: Record<string, unknown> | null;
}

// Search params for messages
export type SearchParams = {
  partition?: number;
  limit: number | 'continuously';
  query?: {
    value: string;
    where: 'headers' | 'key' | 'value' | 'everywhere';
  };
  from:
    | { type: 'timestamp'; value: string }
    | { type: 'epoch'; value: number }
    | { type: 'offset'; value: number }
    | { type: 'latest' };
};

// Rebalance types
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

export interface RebalanceCondition {
  type?: string;
  status?: string;
  reason?: string;
  message?: string;
  lastTransitionTime?: string;
}

export interface Rebalance {
  id: string;
  type: 'kafkaRebalances';
  meta?: {
    autoApproval?: boolean;
    allowedActions: string[];
    privileges?: string[];
    page?: {
      cursor: string;
    };
    managed?: boolean;
  };
  attributes: {
    name: string;
    namespace?: string;
    creationTimestamp: string;
    status: RebalanceStatus | null;
    mode: RebalanceMode;
    brokers: number[] | null;
    sessionId?: string | null;
    optimizationResult?: OptimizationResult;
    conditions?: RebalanceCondition[] | null;
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

export interface RebalanceResponse {
  data: Rebalance;
}