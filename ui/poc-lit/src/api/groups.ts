import { apiClient, ApiResponse } from './client';

export interface MemberDescription {
  memberId: string;
  groupInstanceId?: string | null;
  clientId: string;
  host: string;
  assignments?: Array<{
    topicId?: string;
    topicName: string;
    partition: number;
  }>;
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

export interface ConfigValue {
  value: string;
  source: string;
  sensitive: boolean;
  readOnly: boolean;
  type: string;
}

export interface Group {
  id: string;
  type: 'groups';
  attributes: {
    groupId: string;
    type: 'Classic' | 'Consumer' | 'Share' | 'Streams' | null;
    protocol: string | null;
    state: 'UNKNOWN' | 'PREPARING_REBALANCE' | 'COMPLETING_REBALANCE' | 'STABLE' | 'DEAD' | 'EMPTY' | 'ASSIGNING' | 'RECONCILING';
    simpleConsumerGroup?: boolean;
    members?: MemberDescription[] | null;
    offsets?: OffsetAndMetadata[] | null;
    partitionAssignor?: string | null;
    coordinator?: {
      id: number;
      host: string;
      port: number;
      rack?: string;
    } | null;
    configs?: Record<string, ConfigValue>;
  };
  meta?: {
    privileges?: string[];
    describeAvailable?: boolean;
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

export interface GetGroupsParams {
  id?: string;
  type?: string[];
  consumerGroupState?: string[];
  fields?: string[];
  sort?: string;
  sortDir?: 'asc' | 'desc';
  pageSize?: number;
  pageCursor?: string;
}
export interface GroupResponse {
  data: Group;
}

export async function getGroup(
  kafkaId: string,
  groupId: string,
  params: {
    fields?: string[];
  } = {}
): Promise<ApiResponse<GroupResponse>> {
  const searchParams = new URLSearchParams();
  
  if (params.fields) {
    searchParams.set('fields[groups]', params.fields.join(','));
  }

  return apiClient.get<GroupResponse>(`/kafkas/${kafkaId}/groups/${groupId}?${searchParams}`);
}


export async function getGroups(
  kafkaId: string,
  params: GetGroupsParams = {}
): Promise<ApiResponse<GroupsResponse>> {
  const searchParams = new URLSearchParams();
  
  if (params.id) searchParams.set('filter[id]', `like,*${params.id}*`);
  if (params.type?.length) searchParams.set('filter[type]', `in,${params.type.join(',')}`);
  if (params.consumerGroupState?.length) searchParams.set('filter[state]', `in,${params.consumerGroupState.join(',')}`);
  if (params.fields) searchParams.set('fields[groups]', params.fields.join(','));
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

  return apiClient.get<GroupsResponse>(`/kafkas/${kafkaId}/groups?${searchParams}`);
}

// Made with Bob
