import { SearchParams } from '@/api/types';
import { FromOptions } from './FetchFromSelector';
import { WhereOption } from './whereSelector';

export const DEFAULT_LIMIT = 50;

export interface FilterState {
  partition?: number;
  from: FromOptions;
  offset?: number;
  limit: number;
  query: string;
  where: WhereOption;
  timestamp?: string;
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
