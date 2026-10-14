import { useMemo, useState } from 'react';
import { SearchParams } from '@/api/types';
import { FromOptions } from './MessageFilter/FetchFromSelector';
import {
  applyPatch,
  buildSearchParams,
  DEFAULT_LIMIT,
  FilterState,
  isSameFilter,
} from './MessageFilter/messageFIlter';
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
}

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
}: AdvancedSearchProps) {
  // Initial state comes from the URL; the page already loaded it, so no search here.
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
            : 'latest'),
        offset: filterOffset,
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
    const next = applyPatch(filter, patch);
    if (isSameFilter(filter, next)) return;
    setFilter(next);

    if (
      (next.from === 'offset' && next.offset === undefined) ||
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
