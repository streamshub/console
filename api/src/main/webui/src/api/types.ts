/**
 * API Type Definitions
 * 
 * These types match the API responses from the Quarkus backend.
 * Copied and adapted from the existing Next.js implementation.
 */

// Common types
export interface ListResponse<T extends Resource> {
  meta?: {
    page: {
      total: number;
      pageNumber: number;
      rangeTruncated: boolean;
    } & Record<string, unknown>;
  };
  links?: {
    first?: string;
    last?: string;
    prev?: string;
    next?: string;
  };
  data?: T[];
  errors?: ApiError[];
}

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

export interface MetaWithPrivileges {
  privileges?: string[];
}

export interface Resource {
  type: string;
  id: string;
  attributes?: Record<string, unknown>;
  relationships?: Record<string, { data: ResourceIdentifier | ResourceIdentifier[] }>;
  meta?: object;
}

// Kafka Cluster types
export interface KafkaClusterListener {
  name: string;
  bootstrapServers: string;
  type?: 'internal' | 'external' | string;
  authType?: string;
}

export interface KafkaClusterCondition {
  type?: string;
  status?: string;
  reason?: string;
  message?: string;
  lastTransitionTime?: string;
}

export interface KafkaCluster extends Resource {
  id: string;
  type: 'kafkas';
  attributes: {
    name: string;
    namespace?: string;
    status?: string;
    kafkaVersion?: string;
    creationTimestamp?: string;
    listeners?: KafkaClusterListener[];
    conditions?: KafkaClusterCondition[];
  };
  meta?: MetaWithPrivileges & {
    authentication?: {
      method: string;
    };
    kind?: string;
    managed?: boolean;
    reconciliationPaused?: boolean;
  };
}

export interface KafkaClustersResponse {
  data: KafkaCluster[];
  meta?: MetaWithPrivileges & {
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
  meta?: MetaWithPrivileges & {
    managed?: boolean;
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
  meta: MetaWithPrivileges & {
    page: {
      total: number;
      pageNumber: number;
    };
    summary: {
      statuses: {
        FullyReplicated?: number;
        UnderReplicated?: number;
        PartiallyOffline?: number;
        Offline?: number;
        Unknown?: number;
      };
      totalPartitions: number;
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
  meta?: MetaWithPrivileges & {
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
  meta: MetaWithPrivileges & {
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
  meta?: MetaWithPrivileges;
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

// Metrics types
export interface MetricValue {
  value: string;
  nodeId?: string;
}

export interface MetricRange {
  range: Array<[string, string]>; // [timestamp, value] tuples
  nodeId?: string;
}

export interface Metrics {
  values: Record<string, MetricValue[]>;
  ranges: Record<string, MetricRange[]>;
}

export interface NodeMetricsResponse {
  data: {
    attributes: {
      metrics?: Metrics | null;
    };
  };
}

export interface TopicMetricsResponse {
  data: {
    id: string;
    type: 'topicMetrics';
    attributes: {
      metrics?: Metrics | null;
    };
  };
}

// Time duration options for metrics (in seconds)
export type MetricsDuration = 300 | 900 | 3600 | 21600 | 43200 | 86400; // 5min, 15min, 1hr, 6hr, 12hr, 1d

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
  meta?: MetaWithPrivileges & {
    autoApproval?: boolean;
    allowedActions: string[];
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
  meta: MetaWithPrivileges & {
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

// Kafka Connect types
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

export interface ConnectorConfig {
  [key: string]: string | null | undefined;
}

export interface Plugin {
  class: string;
  type: ConnectorType;
  version: string;
}

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
    config?: ConnectorConfig;
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
    page?: {
      cursor: string;
    };
    managed?: boolean;
  };
}

export interface EnrichedConnector extends Connector {
  connectClusterId: string | null;
  connectClusterName: string | null;
  replicas: number | null;
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
    plugins?: Plugin[];
  };
  meta?: {
    managed: boolean;
    page?: {
      cursor: string;
    };
  };
  relationships?: {
    kafkaClusters?: {
      data: Array<{
        type: 'kafkas';
        id: string;
      }>;
    };
    connectors?: {
      data: Array<{
        type: 'connectors';
        id: string;
      }>;
    };
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

export interface ConnectorTask {
  id: string;
  type: 'connectorTasks';
  attributes: {
    taskId: number;
    config?: ConnectorConfig;
    state: ConnectorState;
    workerId: string;
  };
}

export interface ConnectorDetailResponse {
  data: Connector & {
    relationships: {
      connectCluster: {
        data: {
          type: 'connects';
          id: string;
        };
      };
      tasks: {
        data: Array<{
          type: 'connectorTasks';
          id: string;
        }>;
      };
    };
  };
  included: Array<ConnectCluster | ConnectorTask>;
}

export interface ConnectClusterDetailResponse {
  data: ConnectCluster & {
    relationships: {
      connectors: {
        data: Array<{
          type: 'connectors';
          id: string;
        }>;
      };
    };
  };
  included?: Connector[];
}

// Kafka User types
export interface Authorization {
  type: string;
  resourceName?: string | null;
  patternType?: string | null;
  host?: string | null;
  operations: string[];
  permissionType: string;
}

export interface KafkaUser {
  id: string;
  type: 'kafkaUsers';
  meta?: MetaWithPrivileges;
  attributes: {
    name: string;
    namespace?: string | null;
    creationTimestamp?: string | null;
    username: string;
    authenticationType: string;
    authorization?: {
      accessControls: Authorization[];
    } | null;
  };
  relationships?: Record<string, unknown>;
}

export interface UsersResponse {
  data: KafkaUser[];
  meta: MetaWithPrivileges & {
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

export interface UserResponse {
  data: KafkaUser;
}

// Reset Offset types
export type OffsetValue = 'custom' | 'latest' | 'earliest' | 'specificDateTime' | 'delete';
export type DateTimeFormat = 'ISO' | 'Epoch';
export type TopicSelection = 'allTopics' | 'selectedTopic';
export type PartitionSelection = 'allPartitions' | 'selectedPartition';

export interface OffsetResetRequest {
  topicId: string;
  partition?: number;
  offset: string | number | null;
  metadata?: string;
}

export interface OffsetResetResult {
  topicId?: string;
  topicName: string;
  partition: number;
  offset: number | null;
  metadata?: string;
}

export interface ResetOffsetFormState {
  topicSelection: TopicSelection;
  selectedTopicId?: string;
  selectedTopicName?: string;
  partitionSelection: PartitionSelection;
  selectedPartition?: number;
  offsetValue: OffsetValue;
  customOffset?: number;
  dateTime?: string;
  dateTimeFormat: DateTimeFormat;
}

export type ResetOffsetErrorType =
  | 'CustomOffsetError'
  | 'PartitionError'
  | 'KafkaError'
  | 'SpecificDateTimeNotValidError'
  | 'GeneralError';

export interface ResetOffsetError {
  type: ResetOffsetErrorType;
  message: string;
}