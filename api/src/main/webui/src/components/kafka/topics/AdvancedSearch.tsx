import { useMemo, useRef, useState } from 'react';
import { SearchParams } from '@/api/types';
import { FromOptions } from './MessageFilter/FetchFromSelector';
import {
  applyPatch,
  buildSearchParams,
  DEFAULT_LIMIT,
  FilterState,
  isSameFilter,
  isSearchEnabled,
} from './MessageFilter/messageFilter';
import { MessageTableFilter } from './MessageFilter/MessageTableFilter';

interface AdvancedSearchProps {
  partitions: number;
  filterQuery?: string;
  filterWhere?: 'key' | 'headers' | 'value';
  filterPartition?: number;
  filterOffset?: number;
  filterLimit?: number | 'continuously';
  filterFrom?: FromOptions;
  onSearch: (params: SearchParams, selectedFrom: FromOptions) => void;
  filterTimestamp?: string;
  filterEpoch?: number;
}

const isComplete = (s: FilterState) =>
  !(
    (s.from === 'offset' && s.offset === undefined) ||
    (s.from === 'timestamp' && s.timestamp === undefined) ||
    (s.from === 'epoch' && s.epoch === undefined)
  );

export function AdvancedSearch({
  partitions,
  filterQuery,
  filterWhere,
  filterPartition,
  filterOffset,
  filterLimit,
  filterFrom,
  onSearch,
  filterTimestamp,
  filterEpoch,
}: AdvancedSearchProps) {
  const [filter, setFilter] = useState<FilterState>(() =>
    applyPatch(
      {
        partition: filterPartition,
        from:
          filterFrom ??
          (filterOffset !== undefined
            ? 'offset'
            : filterTimestamp !== undefined
            ? 'timestamp'
            : filterEpoch !== undefined
            ? 'epoch'
            : 'latest'),
        offset: filterOffset,
        timestamp: filterTimestamp,
        epoch: filterEpoch,
        limit: typeof filterLimit === 'number' ? filterLimit : DEFAULT_LIMIT,
        query: filterQuery ?? '',
        where: filterWhere,
      },
      {}
    )
  );
  const filterRef = useRef(filter);

  const partitionList = useMemo(
    () => Array.from({ length: partitions }, (_, i) => i),
    [partitions]
  );

  const run = (state: FilterState) => {
    if (!isComplete(state)) return;
    onSearch(
      buildSearchParams({ ...state, query: state.query.trim() }),
      state.from
    );
  };

  const update = (patch: Partial<FilterState>, applyNow: boolean) => {
    const previous = filterRef.current;
    let next = applyPatch(previous, patch);

    if (!isSearchEnabled(next)) {
      next = { ...next, query: '', where: undefined };
    }

    if (isSameFilter(previous, next)) return;
    filterRef.current = next;
    setFilter(next);

    if (applyNow) run(next);
  };

  return (
    <MessageTableFilter
      partitions={partitionList}
      value={filter}
      onChange={(patch) => update(patch, true)}
      onDraftChange={(patch) => update(patch, false)}
      onSubmit={() => run(filterRef.current)}
    />
  );
}
