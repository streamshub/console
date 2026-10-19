import { SearchParams } from '@/api/types';
import { FromOptions } from './FetchFromSelector';
import { WhereOption } from './whereSelector';
import { KafkaRecord } from '@/api/types';

export type SearchWhere = 'key' | 'headers' | 'value' | undefined;

export const DEFAULT_LIMIT = 50;

export interface FilterState {
  partition?: number;
  from: FromOptions;
  offset?: number;
  limit: number;
  query: string;
  where: WhereOption;
  timestamp?: string;
  epoch?: number;
}

const RELATIVE_MINUTES: Partial<Record<FromOptions, number>> = {
  last5: 5,
  last10: 10,
  last15: 15,
};

/** Applies a change and enforces the rules between fields in one place. */
export function applyPatch(
  current: FilterState,
  patch: Partial<FilterState>
): FilterState {
  const next = { ...current, ...patch };
  if (next.from !== 'offset') {
    next.offset = undefined;
  }
  if (next.from !== 'epoch') next.epoch = undefined;
  if (next.from !== 'timestamp') next.timestamp = undefined;
  if (next.from === 'latest') {
    // The search box is hidden for "latest", so don't keep a hidden filter
    next.query = '';
    next.where = undefined;
  }
  return next;
}

export function isSameFilter(a: FilterState, b: FilterState) {
  return (Object.keys(b) as (keyof FilterState)[]).every((k) => a[k] === b[k]);
}

export function isValidOffset(raw: string) {
  const s = raw.trim();
  return s === '' || (/^\d+$/.test(s) && Number.isSafeInteger(Number(s)));
}

export function parseOffset(raw: string) {
  const s = raw.trim();
  return s === '' ? undefined : Number(s);
}

function buildFrom({
  from,
  offset,
  timestamp,
  epoch,
}: FilterState): SearchParams['from'] {
  const minutes = RELATIVE_MINUTES[from];
  if (minutes !== undefined) {
    return {
      type: 'timestamp',
      value: new Date(Date.now() - minutes * 60_000).toISOString(),
    };
  }
  if (from === 'timestamp' && timestamp !== undefined) {
    return { type: 'timestamp', value: timestamp };
  }
  if (from === 'epoch' && epoch !== undefined) {
    return { type: 'epoch', value: epoch };
  }
  if (from === 'offset' && offset !== undefined) {
    return { type: 'offset', value: offset };
  }
  return { type: 'latest' };
}

export function buildSearchParams(state: FilterState): SearchParams {
  const text = state.query.trim();
  return {
    query: text
      ? { value: text, where: state.where ?? 'everywhere' }
      : undefined,
    partition: state.partition,
    from: buildFrom(state),
    limit: state.limit,
  };
}

export function isSearchEnabled({
  from,
  offset,
  epoch,
  timestamp,
}: Pick<FilterState, 'from' | 'offset' | 'epoch' | 'timestamp'>): boolean {
  switch (from) {
    case 'last5':
    case 'last10':
    case 'last15':
      return true;
    case 'offset':
      return offset !== undefined;
    case 'epoch':
      return epoch !== undefined;
    case 'timestamp':
      return timestamp !== undefined;
    default:
      return false;
  }
}

export function filterMessages(
  messages: KafkaRecord[],
  query: string | undefined,
  where: SearchWhere
): KafkaRecord[] {
  const term = query?.trim().toLowerCase();
  if (!term) {
    return messages;
  }

  const matches = (value: unknown): boolean =>
    value !== null &&
    value !== undefined &&
    String(value).toLowerCase().includes(term);

  return messages.filter((message) => {
    const { key, value, headers } = message.attributes;

    const inKey = () => matches(key);
    const inValue = () => matches(value);
    const inHeaders = () =>
      Object.entries(headers ?? {}).some(
        ([name, headerValue]) => matches(name) || matches(headerValue)
      );

    switch (where) {
      case 'key':
        return inKey();
      case 'value':
        return inValue();
      case 'headers':
        return inHeaders();
      default:
        return inKey() || inValue() || inHeaders();
    }
  });
}

export function isValidEpoch(value: string): boolean {
  if (value.trim() === '') return true;

  const epoch = Number(value);

  return (
    Number.isSafeInteger(epoch) &&
    epoch >= 0 &&
    !isNaN(new Date(epoch).getTime())
  );
}

export function parseEpoch(value: string): number | undefined {
  if (value.trim() === '' || !isValidEpoch(value)) {
    return undefined;
  }

  return Number(value);
}
