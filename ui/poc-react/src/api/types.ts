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
  value: string;
  source: string;
  sensitive: boolean;
  readOnly: boolean;
  type: string;
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
      meta?: Record<string, unknown>;
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

// Consumer Group types
export type ConsumerGroupState =
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

export interface ConsumerGroup {
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
    state: ConsumerGroupState;
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
  };
}

export interface ConsumerGroupsResponse {
  data: ConsumerGroup[];
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
export interface Node {
  id: string;
  type: 'nodes';
  attributes: {
    nodeId: number;
    host?: string;
    port?: number;
    rack?: string;
  };
}

export interface NodesResponse {
  data: Node[];
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