import { useEffect, useMemo, useRef, useState } from 'react';
import { SearchParams } from '@/api/types';
import { WhereOption } from './TopicsFilter/whereSelector';
import { FromOptions } from './TopicsFilter/FetchFromSelector';
import { MessageTableFilter } from './TopicsFilter/MessageTableFilter';

interface AdvancedSearchProps {
  partitions: number;
  filterQuery?: string;
  filterWhere?: 'key' | 'headers' | 'value';
  filterPartition?: number;
  filterOffset?: number;
  filterLimit?: number | 'continuously';
  filterFrom?: FromOptions;
  onSearch: (params: SearchParams, selectedFrom: FromOptions) => void;
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
  filterFrom,
  onSearch,
}: AdvancedSearchProps) {
  const [partition, setPartition] = useState<number | undefined>(
    filterPartition
  );
  const [from, setFrom] = useState<FromOptions>(
    filterFrom ?? (filterOffset !== undefined ? 'offset' : 'latest')
  );
  const [offset, setOffset] = useState<number | undefined>(filterOffset);
  const [limit, setLimit] = useState<number>(
    typeof filterLimit === 'number' ? filterLimit : DEFAULT_LIMIT
  );
  const [query, setQuery] = useState(filterQuery ?? '');
  const [appliedQuery, setAppliedQuery] = useState(filterQuery ?? '');
  const [where, setWhere] = useState<WhereOption>(filterWhere);

  const [offsetInput, setOffsetInput] = useState(
    filterOffset?.toString() ?? ''
  );

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
  const key = JSON.stringify({
    partition,
    from,
    offset,
    limit,
    query: appliedQuery,
    where,
  });
  if (lastSearchedKey.current === null) {
    lastSearchedKey.current = key;
  }

  const commitOffset = () => {
    const trimmed = offsetInput.trim();

    if (trimmed === '') {
      setOffset(undefined);
      return;
    }

    const parsed = Number(trimmed);

    if (Number.isInteger(parsed) && parsed >= 0) {
      setOffset(parsed);
    }
  };

  const commitQuery = () => {
    setAppliedQuery(query.trim());
  };

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
        buildSearchParams({
          partition,
          from,
          offset,
          limit,
          query: appliedQuery,
          where,
        }),
        from
      );
    }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [key, partition, from, offset, limit, appliedQuery, where]);

  const handleFromChange = (next: FromOptions) => {
    setFrom(next);
    if (next !== 'offset') {
      setOffset(undefined);
      setOffsetInput('');
    }
    if (next === 'latest') {
      setQuery('');
      setAppliedQuery('');
      setWhere(undefined);
    }
  };

  return (
    <MessageTableFilter
      partitions={partitionList}
      selectedPartition={partition}
      onPartitionChange={setPartition}
      selectedFrom={from}
      onFromChange={handleFromChange}
      offset={offset}
      onOffsetChange={setOffset}
      offsetInput={offsetInput}
      onOffsetInputChange={setOffsetInput}
      onOffsetCommit={commitOffset}
      limit={limit}
      onLimitChange={setLimit}
      query={query}
      onQueryChange={setQuery}
      where={where}
      onWhereChange={setWhere}
      onQueryCommit={commitQuery}
    />
  );
}
