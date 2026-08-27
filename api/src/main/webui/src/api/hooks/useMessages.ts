/**
 * React Query hooks for fetching Kafka messages/records
 */

import { useCallback, useRef, useState } from 'react';
import {
  experimental_streamedQuery as streamedQuery,
  useQuery,
  UseQueryOptions,
} from '@tanstack/react-query';
import { apiClient, ApiError } from '../client';
import { KafkaRecord, KafkaRecordsResponse } from '../types';

const DEFAULT_PAGE_SIZE = 50;

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
  maxValueLength?: number;
}

/**
 * Escape special like-pattern characters in a raw user query string so that
 * literal text is matched verbatim when the pattern is later wrapped with `*`.
 *
 * The server-side `like` operator treats `*` as "any sequence", `?` as "any
 * single character", and `\` as the escape character.  A plain-text search
 * must not accidentally activate these wildcards.
 */
function escapeLikePattern(raw: string): string {
  return raw.replace(/[\\*?]/g, (ch) => `\\${ch}`);
}

/**
 * Build an `ilike,*<escaped-query>*` (case-insensitive like) operand 
 * value for the given query string, producing case-insensitive substring
 * semantics on the server side.
 */
function likeContains(query: string): string {
  return `ilike,*${escapeLikePattern(query)}*`;
}

/**
 * Append server-side search filter parameters for the given query and target
 * field scope.  The `like` operator is used so the full pre-truncation bytes
 * are scanned on the server.
 *
 * - 'key'     → filter[key] only
 * - 'value'   → filter[value] only
 * - 'headers' → filter[headers] only
 * - undefined → all three (OR semantics: any matching field causes a hit)
 */
function appendSearchFilters(
  searchParams: URLSearchParams,
  query: string,
  where: GetMessagesParams['where'],
): void {
  const pattern = likeContains(query);
  if (where === 'key') {
    searchParams.append('filter[key]', pattern);
  } else if (where === 'value') {
    searchParams.append('filter[value]', pattern);
  } else if (where === 'headers') {
    searchParams.append('filter[headers]', pattern);
  } else {
    // "everywhere" — send all three; server applies OR across them
    searchParams.append('filter[key]', pattern);
    searchParams.append('filter[value]', pattern);
    searchParams.append('filter[headers]', pattern);
  }
}

/**
 * Why the server stopped scanning:
 * - complete:  all requested records were read
 * - limit:     the requested number of matches was found
 * - timeout:   time ran out before all records were read
 * - cancelled: the server stopped before completing
 */
export type EndReason = 'complete' | 'limit' | 'timeout' | 'cancelled';

/** How far the server has got in scanning the topic. */
export interface ScanState {
  /** Records scanned so far, matching or not */
  scanned: number;
  /** Estimated percentage (0-100) of the requested offset range scanned */
  progress: number;
  /** Set when the server reported that it finished; undefined while running */
  endReason?: EndReason;
}

const INITIAL_SCAN_STATE: ScanState = { scanned: 0, progress: 0 };

/**
 * Stream records from the `text/event-stream` records endpoint, yielding each
 * batch as it arrives until the terminal `done` event (or end of stream).
 *
 * The server emits:
 *   data: [{...KafkaRecord...}, ...]   (one event per batch with matches)
 *   event: heartbeat
 *   data: {"scanned":<n>,"progress":<p>}   (after every poll batch)
 *   event: done
 *   data: {"reason":"complete|limit|timeout|cancelled"}
 *
 * `onHeartbeat` receives the total number of records scanned so far, matching
 * or not, and the estimated scan progress each time a heartbeat is received.
 * `onDone` receives the reason given in the terminal event.
 */
async function* streamRecords(
  url: string,
  signal?: AbortSignal,
  onHeartbeat?: (scanned: number, progress: number | undefined) => void,
  onDone?: (reason: EndReason | undefined) => void
): AsyncGenerator<KafkaRecord[]> {
  let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;

  try {
    const response = await fetch(url, {
      method: 'GET',
      signal,
      headers: {
        Accept: 'text/event-stream',
        /*
         * Quarkus uses X-Requested-With for JavaScript requests
         * to use HTTP status 499 when a session is required.
         */
        'X-Requested-With': 'JavaScript',
      },
    });

    if (response.status === 499 && response.headers.get('WWW-Authenticate') === 'OIDC') {
      apiClient.login();
      return;
    }

    if (!response.ok) {
      const body = await response.json().catch(() => undefined);
      throw new ApiError(response.status, response.statusText, body?.errors);
    }

    reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        return;
      }

      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');

      // SSE frames are separated by a blank line (\n\n).
      // Split and keep any incomplete trailing frame in the buffer.
      const frames = buffer.split('\n\n');
      buffer = frames.pop() ?? '';

      for (const frame of frames) {
        let eventName: string | undefined;
        const dataLines: string[] = [];

        for (const line of frame.split('\n')) {
          if (line.startsWith('event:')) {
            eventName = line.slice('event:'.length).trim();
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice('data:'.length).trimStart());
          }
        }

        if (eventName === 'done') {
          let reason: EndReason | undefined;
          try {
            reason = (JSON.parse(dataLines.join('\n')) as { reason?: EndReason }).reason;
          } catch {
            // Malformed done event - the reason is unknown
          }
          onDone?.(reason);
          return;
        }

        if (eventName === 'heartbeat') {
          try {
            const { scanned, progress } = JSON.parse(dataLines.join('\n')) as {
              scanned?: number;
              progress?: number;
            };
            if (typeof scanned === 'number') {
              onHeartbeat?.(scanned, typeof progress === 'number' ? progress : undefined);
            }
          } catch {
            // Malformed heartbeat - ignore
          }
          continue;
        }

        if (dataLines.length > 0) {
          let batch: KafkaRecord[];

          try {
            batch = JSON.parse(dataLines.join('\n')) as KafkaRecord[];
          } catch {
            // Malformed JSON - skip this batch
            continue;
          }

          yield batch;
        }
      }
    }
  } catch (error) {
    if (signal?.aborted) {
      // Aborted by the caller: end the stream, keeping what was received so far
      return;
    }
    throw error;
  } finally {
    // Closes the connection if the consumer stops early (e.g. query cancelled)
    reader?.cancel().catch(() => undefined);
  }
}

function buildRecordsUrl(params: GetMessagesParams): string {
  const {
    kafkaId,
    topicId,
    pageSize = DEFAULT_PAGE_SIZE,
    partition,
    offset,
    timestamp,
    epoch,
    query,
    where,
    maxValueLength,
  } = params;

  const searchParams = new URLSearchParams({
    'fields[records]': 'partition,offset,timestamp,timestampType,headers,key,keySchema,value,valueSchema,size',
    'page[size]': String(pageSize),
  });

  if (maxValueLength !== undefined) {
    searchParams.append('maxValueLength', String(maxValueLength));
  }

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

  if (query && query.length > 0) {
    appendSearchFilters(searchParams, query, where);
  }

  return `/api/kafkas/${kafkaId}/topics/${topicId}/records?${searchParams.toString()}`;
}

/**
 * Order records the way the server orders a result page: by timestamp,
 * partition and offset; newest first when fetching the "latest" records
 * (no offset or timestamp given), oldest first otherwise.
 */
function compareRecords(newestFirst: boolean) {
  const direction = newestFirst ? -1 : 1;
  return (a: KafkaRecord, b: KafkaRecord): number =>
    direction *
    (Date.parse(a.attributes.timestamp) - Date.parse(b.attributes.timestamp) ||
      a.attributes.partition - b.attributes.partition ||
      a.attributes.offset - b.attributes.offset);
}

/**
 * Fetch a single message by partition and offset using the plain JSON
 * endpoint (not SSE), as there is no search to stream results for.
 */
export async function getMessage(
  kafkaId: string,
  topicId: string,
  partition: number,
  offset: number
): Promise<KafkaRecord | undefined> {
  const response = await apiClient.get<KafkaRecordsResponse>(
    buildRecordsUrl({
      kafkaId,
      topicId,
      pageSize: 1,
      partition,
      offset,
    })
  );

  const messages = response.data || [];
  return messages.length === 1 ? messages[0] : undefined;
}

/**
 * Hook to fetch messages for a topic.
 *
 * Records are always retrieved from the SSE endpoint and are added to the
 * query data as each batch arrives, so the table can be populated while the
 * server is still scanning. `isFetching` remains true until the stream ends.
 *
 * `cancel` stops the running stream (closing the connection so the server
 * stops scanning) while keeping the records received so far. `stopped` is true
 * when the current (or last) stream ended because it was cancelled.
 *
 * `scan` reports how far the server has got in the current (or last) stream:
 * the number of records scanned (whether or not they matched the search), the
 * estimated progress, and the reason the server finished once it has.
 */
export function useMessages(
  params: GetMessagesParams,
  options?: Omit<UseQueryOptions<KafkaRecord[], ApiError>, 'queryKey' | 'queryFn'>
) {
  const pageSize = params.pageSize ?? DEFAULT_PAGE_SIZE;
  const newestFirst =
    params.offset === undefined &&
    !params.timestamp &&
    params.epoch === undefined;
  const controllerRef = useRef<AbortController | null>(null);
  const [scan, setScan] = useState<ScanState>(INITIAL_SCAN_STATE);
  const [stopped, setStopped] = useState(false);

  const cancel = useCallback(() => {
    controllerRef.current?.abort();
    setStopped(true);
  }, []);

  const query = useQuery({
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
      params.maxValueLength,
    ],
    // The streamed query function is created when the query runs, not during render
    queryFn: (context) =>
      streamedQuery<KafkaRecord[], KafkaRecord[]>({
        streamFn: ({ signal }) => {
          /*
           * Use a controller of our own so the stream can be ended without
           * cancelling the query, which would discard the received records.
           */
          const controller = new AbortController();
          controllerRef.current = controller;
          signal.addEventListener('abort', () => controller.abort());
          setScan(INITIAL_SCAN_STATE);
          setStopped(false);
          return streamRecords(
            buildRecordsUrl(params),
            controller.signal,
            (scanned, progress) =>
              setScan((prev) => ({ ...prev, scanned, progress: progress ?? prev.progress })),
            (endReason) =>
              setScan((prev) => ({
                ...prev,
                endReason,
                // A finished scan has covered everything it set out to read
                progress: endReason === 'complete' ? 100 : prev.progress,
              }))
          );
        },
        /*
         * When polling (continuous mode) keep the previous records on screen
         * until the new stream completes to avoid flicker. Otherwise start from
         * an empty table and fill it as records arrive.
         */
        refetchMode: options?.refetchInterval ? 'replace' : 'reset',
        initialValue: [],
        reducer: (records, batch) =>
          [...records, ...batch]
            .sort(compareRecords(newestFirst))
            .slice(0, pageSize),
      })(context),
    ...options,
  });

  return { query, cancel, scan, stopped };
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
  return useQuery({
    queryKey: ['message', kafkaId, topicId, partition, offset],
    queryFn: () =>
      partition !== undefined && offset !== undefined
        ? getMessage(kafkaId, topicId, partition, offset)
        : Promise.resolve(undefined),
    enabled: partition !== undefined && offset !== undefined,
    ...options,
  });
}
