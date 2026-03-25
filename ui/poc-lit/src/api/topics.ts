import { apiClient, ApiResponse } from './client';

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
      meta?: Record<string, any>;
      data?: any[];
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

export interface TopicResponse {
  data: Topic;
}

export async function getTopics(
  kafkaId: string,
  params: {
    page?: number;
    perPage?: number;
    sort?: string;
    sortDir?: 'asc' | 'desc';
    name?: string;
    fields?: string[];
  } = {}
): Promise<ApiResponse<TopicsResponse>> {
  const searchParams = new URLSearchParams();
  
  if (params.page) searchParams.set('page[number]', params.page.toString());
  if (params.perPage) searchParams.set('page[size]', params.perPage.toString());
  if (params.sort) searchParams.set('sort', `${params.sortDir === 'desc' ? '-' : ''}${params.sort}`);
  if (params.name) searchParams.set('filter[name]', `like,*${params.name}*`);
  if (params.fields) searchParams.set('fields[topics]', params.fields.join(','));

  const query = searchParams.toString();
  const path = `/kafkas/${kafkaId}/topics${query ? `?${query}` : ''}`;
  
  return apiClient.get<TopicsResponse>(path);
}

// Made with Bob

export async function getTopic(
  kafkaId: string,
  topicId: string,
  params: {
    fields?: string[];
  } = {}
): Promise<ApiResponse<TopicResponse>> {
  const searchParams = new URLSearchParams();
  
  const defaultFields = 'name,status,visibility,partitions,numPartitions,authorizedOperations,configs,totalLeaderLogBytes,groups';
  searchParams.set('fields[topics]', params.fields?.join(',') || defaultFields);

  return apiClient.get<TopicResponse>(`/kafkas/${kafkaId}/topics/${topicId}?${searchParams}`);
}
