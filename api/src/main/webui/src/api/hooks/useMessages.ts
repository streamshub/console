/**
 * React Query hooks for fetching Kafka messages/records
 */

import { useQuery, UseQueryOptions } from '@tanstack/react-query';
import { apiClient } from '../client';
import { KafkaRecord, KafkaRecordsResponse, ApiError } from '../types';

interface GetMessagesParams {
  kafkaId: string;
  topicId: string;
  pageSize?: number;
  partition?: number;
  offset?: number;
  timestamp?: string;
  epoch?: number;
  query?: string;
  where?: 'key' | 'headers' | 'value';
}

/**
 * Fetch messages for a topic
 */
export async function getMessages(params: GetMessagesParams): Promise<KafkaRecord[]> {
  const {
    kafkaId,
    topicId,
    pageSize = 50,
    partition,
    offset,
    timestamp,
    epoch,
    query,
    where,
  } = params;

  const searchParams = new URLSearchParams({
    'fields[records]': 'partition,offset,timestamp,timestampType,headers,key,keySchema,value,valueSchema,size',
    'page[size]': String(pageSize),
  });

  if (partition !== undefined) {
    searchParams.append('filter[partition]', String(partition));
  }

  if (offset !== undefined) {
    searchParams.append('filter[offset]', `gte,${offset}`);
  }

  if (timestamp) {
    searchParams.append('filter[timestamp]', `gte,${timestamp}`);
  } else if (epoch !== undefined) {
    // Convert epoch to ISO timestamp
    const date = new Date(Number.isInteger(epoch) ? epoch * 1000 : epoch);
    searchParams.append('filter[timestamp]', `gte,${date.toISOString()}`);
  }

  const response = await apiClient.get<KafkaRecordsResponse>(
    `/api/kafkas/${kafkaId}/topics/${topicId}/records?${searchParams.toString()}`
  );

  let messages = response.data || [];

  // Client-side filtering for query
  if (query && query.length > 0) {
    const lowerQuery = query.toLowerCase();
    messages = messages.filter((m) => {
      if (where === 'key' || where === undefined) {
        if (m.attributes.key?.toLowerCase().includes(lowerQuery)) return true;
      }
      if (where === 'value' || where === undefined) {
        if (m.attributes.value?.toLowerCase().includes(lowerQuery)) return true;
      }
      if (where === 'headers' || where === undefined) {
        if (JSON.stringify(m.attributes.headers).toLowerCase().includes(lowerQuery)) return true;
      }
      return false;
    });
  }

  return messages;
}

/**
 * Fetch a single message by partition and offset
 */
export async function getMessage(
  kafkaId: string,
  topicId: string,
  partition: number,
  offset: number
): Promise<KafkaRecord | undefined> {
  const searchParams = new URLSearchParams({
    'fields[records]': 'partition,offset,timestamp,timestampType,headers,key,keySchema,value,valueSchema,size',
    'filter[partition]': String(partition),
    'filter[offset]': `gte,${offset}`,
    'page[size]': '1',
  });

  const response = await apiClient.get<KafkaRecordsResponse>(
    `/api/kafkas/${kafkaId}/topics/${topicId}/records?${searchParams.toString()}`
  );

  const messages = response.data || [];
  return messages.length === 1 ? messages[0] : undefined;
}

/**
 * Hook to fetch messages for a topic
 */
export function useMessages(
  params: GetMessagesParams,
  options?: Omit<UseQueryOptions<KafkaRecord[], ApiError>, 'queryKey' | 'queryFn'>
) {
  return useQuery<KafkaRecord[], ApiError>({
    queryKey: [
      'messages',
      params.kafkaId,
      params.topicId,
      params.pageSize,
      params.partition,
      params.offset,
      params.timestamp,
      params.epoch,
      params.query,
      params.where,
    ],
    queryFn: () => getMessages(params),
    ...options,
  });
}

/**
 * Hook to fetch a single message
 */
export function useMessage(
  kafkaId: string,
  topicId: string,
  partition: number | undefined,
  offset: number | undefined,
  options?: Omit<UseQueryOptions<KafkaRecord | undefined, ApiError>, 'queryKey' | 'queryFn'>
) {
  return useQuery<KafkaRecord | undefined, ApiError>({
    queryKey: ['message', kafkaId, topicId, partition, offset],
    queryFn: () =>
      partition !== undefined && offset !== undefined
        ? getMessage(kafkaId, topicId, partition, offset)
        : Promise.resolve(undefined),
    enabled: partition !== undefined && offset !== undefined,
    ...options,
  });
}