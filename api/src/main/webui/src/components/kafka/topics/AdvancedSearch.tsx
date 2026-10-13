import { useEffect, useMemo, useRef, useState } from 'react';
import { SearchParams } from '@/api/types';
import { WhereOption } from './TopicsFilter/whereSelector';
import { FromOptions } from './TopicsFilter/FetchFromSelector';
import { NewFilter } from './TopicsFilter/MessageTableFilter';

interface AdvancedSearchProps {
  partitions: number;
  filterQuery?: string;
  filterWhere?: 'key' | 'headers' | 'value';
  filterPartition?: number;
  filterOffset?: number;
  filterLimit?: number | 'continuously';
  onSearch: (params: SearchParams) => void;
}

const DEFAULT_LIMIT = 50;
const SEARCH_DEBOUNCE_MS = 400;

const RELATIVE_MINUTES: Partial<Record<FromOptions, number>> = {
  last5: 5,
  last10: 10,
  last15: 15,
};

interface FilterState {
  partition?: number;
  from: FromOptions;
  offset?: number;
  limit: number;
  query: string;
  where: WhereOption;
}

function buildSearchParams({
  partition,
  from,
  offset,
  limit,
  query,
  where,
}: FilterState): SearchParams {
  const minutes = RELATIVE_MINUTES[from];

  const fromParam: SearchParams['from'] =
    minutes !== undefined
      ? {
          // "Last N minutes": start from now minus N minutes
          type: 'timestamp',
          value: new Date(Date.now() - minutes * 60_000).toISOString(),
        }
      : from === 'offset' && offset !== undefined
      ? { type: 'offset', value: offset }
      : { type: 'latest' };

  // The text search is only part of the filter when not fetching "latest"
  const hasQuery = from !== 'latest' && query.trim() !== '';

  return {
    query: hasQuery
      ? { value: query, where: where ?? 'everywhere' }
      : undefined,
    partition,
    from: fromParam,
    limit,
  };
}

export function AdvancedSearch({
  partitions,
  filterQuery,
  filterWhere,
  filterPartition,
  filterOffset,
  filterLimit,
  onSearch,
}: AdvancedSearchProps) {
  const [partition, setPartition] = useState<number | undefined>(
    filterPartition
  );
  const [from, setFrom] = useState<FromOptions>(
    filterOffset !== undefined ? 'offset' : 'latest'
  );
  const [offset, setOffset] = useState<number | undefined>(filterOffset);
  const [limit, setLimit] = useState<number>(
    typeof filterLimit === 'number' ? filterLimit : DEFAULT_LIMIT
  );
  const [query, setQuery] = useState(filterQuery ?? '');
  const [where, setWhere] = useState<WhereOption>(filterWhere);

  // NewFilter wants the list of partitions; callers still pass a count
  const partitionList = useMemo(
    () => Array.from({ length: partitions }, (_, i) => i),
    [partitions]
  );

  // Keep the latest callback without re-running the search effect
  const onSearchRef = useRef(onSearch);
  useEffect(() => {
    onSearchRef.current = onSearch;
  }, [onSearch]);

  // Run the search whenever the selection changes (debounced for typing).
  // The initial selection is not searched: the page already loaded it.
  const lastSearchedKey = useRef<string | null>(null);
  const key = JSON.stringify({ partition, from, offset, limit, query, where });
  if (lastSearchedKey.current === null) {
    lastSearchedKey.current = key;
  }

  useEffect(() => {
    if (key === lastSearchedKey.current) {
      return;
    }
    // Offset selected but not typed yet: nothing to search for
    if (from === 'offset' && offset === undefined) {
      return;
    }

    const timer = setTimeout(() => {
      lastSearchedKey.current = key;
      onSearchRef.current(
        buildSearchParams({ partition, from, offset, limit, query, where })
      );
    }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [key, partition, from, offset, limit, query, where]);

  const handleFromChange = (next: FromOptions) => {
    setFrom(next);
    if (next !== 'offset') {
      setOffset(undefined);
    }
    if (next === 'latest') {
      // The search box is hidden for "latest", so don't keep a hidden filter
      setQuery('');
      setWhere(undefined);
    }
  };

  return (
    <NewFilter
      partitions={partitionList}
      selectedPartition={partition}
      onPartitionChange={setPartition}
      selectedFrom={from}
      onFromChange={handleFromChange}
      offset={offset}
      onOffsetChange={setOffset}
      limit={limit}
      onLimitChange={setLimit}
      query={query}
      onQueryChange={setQuery}
      where={where}
      onWhereChange={setWhere}
    />
  );
}
