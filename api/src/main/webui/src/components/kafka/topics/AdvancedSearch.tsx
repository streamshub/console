import { useMemo, useState } from 'react';
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
  filterEpoch?: number;
  onSearch: (params: SearchParams, selectedFrom: FromOptions) => void;
  filterTimestamp?: string;
}

export function AdvancedSearch({
  partitions,
  filterQuery,
  filterWhere,
  filterPartition,
  filterOffset,
  filterLimit,
  filterFrom,
  filterEpoch,
  onSearch,
  filterTimestamp,
}: AdvancedSearchProps) {
  const [filter, setFilter] = useState<FilterState>(() =>
    applyPatch(
      {
        partition: filterPartition,
        from:
          filterFrom ??
          (filterOffset !== undefined
            ? 'offset'
            : filterEpoch !== undefined
            ? 'epoch'
            : filterTimestamp !== undefined
            ? 'timestamp'
            : 'latest'),
        offset: filterOffset,
        epoch: filterEpoch,
        timestamp: filterTimestamp,
        limit: typeof filterLimit === 'number' ? filterLimit : DEFAULT_LIMIT,
        query: filterQuery ?? '',
        where: filterWhere,
      },
      {}
    )
  );

  const partitionList = useMemo(
    () => Array.from({ length: partitions }, (_, i) => i),
    [partitions]
  );

  const handleChange = (patch: Partial<FilterState>) => {
    let next = applyPatch(filter, patch);

    if (!isSearchEnabled(next)) {
      next = { ...next, query: '', where: undefined };
    }

    if (isSameFilter(filter, next)) return;
    setFilter(next);

    if (
      (next.from === 'offset' && next.offset === undefined) ||
      (next.from === 'epoch' && next.epoch === undefined) ||
      (next.from === 'timestamp' && next.timestamp === undefined)
    ) {
      return;
    }

    onSearch(buildSearchParams(next), next.from);
  };

  return (
    <MessageTableFilter
      partitions={partitionList}
      value={filter}
      onChange={handleChange}
    />
  );
}
