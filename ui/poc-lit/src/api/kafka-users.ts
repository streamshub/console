import { apiClient, ApiResponse } from './client';

export interface KafkaUser {
  id: string;
  type: 'kafkaUsers';
  attributes: {
    name: string;
    namespace?: string | null;
    creationTimestamp?: string | null;
    username: string;
    authenticationType: string;
    authorization?: {
      accessControls: Array<{
        type: string;
        resourceName?: string | null;
        patternType?: string | null;
        host?: string | null;
        operations: string[];
        permissionType: string;
      }>;
    } | null;
  };
  meta: {
    privileges?: string[];
  };
}

export interface KafkaUsersResponse {
  data: KafkaUser[];
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

export interface GetKafkaUsersParams {
  username?: string;
  sort?: string;
  sortDir?: 'asc' | 'desc';
  pageSize?: number;
  pageCursor?: string;
}

export async function getKafkaUsers(
  kafkaId: string,
  params: GetKafkaUsersParams = {}
): Promise<ApiResponse<KafkaUsersResponse>> {
  const searchParams = new URLSearchParams();
  
  if (params.username) searchParams.set('filter[username][contains]', params.username);
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

  return apiClient.get<KafkaUsersResponse>(`/kafkas/${kafkaId}/users?${searchParams}`);
}

// Made with Bob
