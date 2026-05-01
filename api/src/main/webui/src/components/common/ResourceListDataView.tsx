import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  DataView,
  DataViewTable,
  DataViewToolbar,
  DataViewTextFilter,
  useDataViewFilters,
  useDataViewPagination,
  DataViewEventsProvider,
  DataViewTr,
  DataViewState,
  useDataViewSort,
} from '@patternfly/react-data-view';
/*
 * The following import is a work-around for
 * https://github.com/patternfly/react-data-view/issues/662
 * and should be removed when upgrading to a version of react-data-view
 * that includes the fix. The DataViewTh import should be moved above to
 * be from '@patternfly/react-data-view'.
 */
import { DataViewTh } from '@patternfly/react-data-view/dist/cjs/DataViewTable';
import {
  Pagination,
  EmptyState,
  EmptyStateBody,
} from '@patternfly/react-core';
import { SkeletonTableBody, SkeletonTableHead } from '@patternfly/react-component-groups';
import { SearchIcon, CubesIcon, ErrorCircleOIcon } from '@patternfly/react-icons';
import { ISortBy, Tbody, Td, Tr } from '@patternfly/react-table';
import { ListResponse, Resource } from '@/api/types';
import { DataViewFilters } from '@patternfly/react-data-view/dist/dynamic/DataViewFilters';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import { UseQueryResult } from '@tanstack/react-query';
import { ApiError } from '@/api/client';

const perPageOptions = [
  { title: '5', value: 5 },
  { title: '10', value: 10 },
  { title: '20', value: 20 },
  { title: '50', value: 50 },
  { title: '100', value: 100 },
];

const DEFAULT_PAGE_SIZE = 10;

// Component wrapper for text filter to avoid hooks-in-callback issue
const TextFilterWrapper: React.FC<{
  name: string;
  filter: {
    type: 'text' | 'checkbox';
    title: string;
    placeholder: string;
    initialValue?: string | string[];
  };
  pendingFilters: Record<string, string>;
  setPendingFilters: React.Dispatch<React.SetStateAction<Record<string, string>>>;
  onSetFilters: (newFilters: Partial<Record<string, string | string[]>>) => void;
}> = ({ name, filter, pendingFilters, setPendingFilters, onSetFilters }) => {
  const handleChange = (_event: React.FormEvent<HTMLInputElement> | undefined, value: string) => {
    if (value) {
      setPendingFilters(prev => ({ ...prev, [name]: value }));
    } else {
      onSetFilters({ [name]: '' });
      setPendingFilters(prev => {
        const { [name]: _, ...rest } = prev;
        return rest;
      });
    }
  };

  const handleClear = () => {
    onSetFilters({ [name]: '' });
    setPendingFilters(prev => {
      const { [name]: _, ...rest } = prev;
      return rest;
    });
  };

  const handleSearch = (_event: React.SyntheticEvent<HTMLButtonElement>, value: string) => {
    const filterValue = pendingFilters[name] ?? value;
    onSetFilters({ [name]: filterValue });
    setPendingFilters(prev => {
      const { [name]: _, ...rest } = prev;
      return rest;
    });
  };

  return (
    <DataViewTextFilter
      filterId={name}
      title={filter.title}
      placeholder={filter.placeholder}
      onChange={handleChange}
      onClear={handleClear}
      onSearch={handleSearch}
    />
  );
};

export interface ResourceListDataViewColumnMapper {
  (
    sortBy?: string, 
    direction?: ISortBy['direction'],
    onSort?: (
      _event: React.MouseEvent | React.KeyboardEvent | MouseEvent | undefined,
      newSortBy: string,
      newSortDirection: ISortBy["direction"]
    ) => void,
  ): DataViewTh[];
}

export interface ResourceListDataViewRowMapper<T extends Resource> {
  (entity: T): DataViewTr;
}

export interface ResourceListDataViewProps<T extends Resource> {
  resourceResult: UseQueryResult<ListResponse<T>, Error>;
  columnProvider: {
    dependencies: unknown[];
    callback: ResourceListDataViewColumnMapper;
  };
  rowProvider: {
    dependencies: unknown[];
    callback: ResourceListDataViewRowMapper<T>;
  };
  dataFilters?: Record<string, {
    type: 'text' | 'checkbox';
    title: string;
    placeholder: string;
    initialValue?: string | string[];
  }>;
  ariaLabel?: string;
  ouiaIdPrefix?: string;
  onDataViewChange: (params: ResourceListParams) => void;
}

export function ResourceListDataView<T extends Resource>({
  resourceResult,
  columnProvider,
  rowProvider,
  dataFilters,
  ariaLabel,
  ouiaIdPrefix = 'noid',
  onDataViewChange,
}: ResourceListDataViewProps<T>) {

  const { t } = useTranslation();
  const [ searchParams, setSearchParams ] = useSearchParams();

  // Map user-provided filter config to empty initial values
  const initialFilters = useMemo(() => {
    if (!dataFilters) {
      return {};
    }
    
    return Object.entries(dataFilters).reduce((acc, [filterId, config]) => {
      // Initialize based on filter type
      if (config.type === 'checkbox') {
        // Empty array for checkbox filters
        acc[filterId] = config.initialValue ?? [];
      } else {
        // Empty string for text filters
        acc[filterId] = config.initialValue ?? '';
      }
      return acc;
    }, {} as Record<string, string | string[]>);
  }, [ dataFilters ]);

  const {
    filters,
    onSetFilters,
    clearAllFilters
  } = useDataViewFilters<Record<string, string | string[]>>({
    initialFilters,
    searchParams,
    setSearchParams: () => {
      /* Do nothing (block the hook's update of the params).
       *
       * The `useDataViewFilters` hook requires both search param functions
       * in order to retrieve current filters from the URL. URL updates are handled
       * separately in a useEffect below to avoid race conditions between the hooks. */
    },
  });

  const [ pendingFilters, setPendingFilters ] = useState<Record<string, string>>({});
  const [ beforeCursor, setBeforeCursor ] = useState<string | undefined>(
    searchParams.get("page[before]") ?? undefined
  );
  const [ afterCursor, setAfterCursor ] = useState<string | undefined>(
    searchParams.get("page[after]") ?? undefined
  );

  /*
   * synthetic URLSearchParams to convert single sort parameter
   * to two parameters expected by the hook
   */
  const sortSearchParams = useMemo(() => {
    const params = new URLSearchParams();
    const sort = searchParams.get("sort");
    if (sort) {
      let sortBy: string;

      if (sort.startsWith("-")) {
        params.set("direction", "desc");
        sortBy = sort.substring(1);
      } else {
        params.set("direction", "asc");
        sortBy = sort;
      }

      params.set("sortBy", sortBy);
    }
    return params;
  }, [searchParams]);

  const { sortBy, direction, onSort } = useDataViewSort({
    searchParams: sortSearchParams,
  });

  const listResponse = resourceResult.data;
  const totalCount = listResponse?.meta?.page?.total ?? 0;

  // Track current page number locally to avoid flashing during navigation
  const [currentPage, setCurrentPage] = useState(listResponse?.meta?.page?.pageNumber ?? 1);

  // Update current page when API response arrives
  useEffect(() => {
    const pageNumber = listResponse?.meta?.page?.pageNumber;
    if (pageNumber) {
      setTimeout(() => setCurrentPage(pageNumber), 0);
    }
  }, [listResponse?.meta?.page?.pageNumber]);

  // Reset to page 1 when filters or sort change
  useEffect(() => {
    const hasFilters = Object.values(filters).some(v =>
      Array.isArray(v) ? v.length > 0 : v?.trim().length > 0
    );
    if (hasFilters || sortBy) {
      setTimeout(() => setCurrentPage(listResponse?.meta?.page?.pageNumber ?? 1), 0);
    }
  }, [filters, sortBy, listResponse?.meta?.page?.pageNumber]);

  /*
   * synthetic URLSearchParams that includes the current page number.
   * Uses local state to avoid flashing during navigation.
   */
  const pageSize = useMemo(() => searchParams.get('page[size]'), [searchParams]);
  const paginationSearchParams = useMemo(() => {
    const params = new URLSearchParams();
    params.set('page', String(currentPage));

    // Only copy the page size param if it exists
    if (pageSize) {
      params.set('page[size]', pageSize);
    }

    return params;
  }, [currentPage, pageSize]);

  // DataView manages pagination state internally
  const pagination = useDataViewPagination({
    perPage: DEFAULT_PAGE_SIZE,
    perPageParam: 'page[size]',
    searchParams: paginationSearchParams,
  });

  const { perPage } = pagination;

  // Memoized pagination handlers
  const handlePerPageSelect = useCallback((event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setBeforeCursor(undefined);
    setAfterCursor(undefined);
    pagination.onPerPageSelect(event, newPerPage);
  }, [pagination]);

  const handleNextPage = useCallback(() => {
    const nextPage = listResponse?.links?.next;
    const cursor = new URLSearchParams(nextPage).get("page[after]") ?? undefined;
    setBeforeCursor(undefined);
    setAfterCursor(cursor);
  }, [listResponse?.links?.next]);

  const handlePreviousPage = useCallback(() => {
    const prevPage = listResponse?.links?.prev;
    const cursor = new URLSearchParams(prevPage).get("page[before]") ?? undefined;
    setBeforeCursor(cursor);
    setAfterCursor(undefined);
  }, [listResponse?.links?.prev]);

  // Memoized filter handlers
  const handleClearAllFilters = useCallback(() => {
    clearAllFilters();
    setPendingFilters({});
  }, [clearAllFilters]);

  const handleFilterChange = useCallback((_key: string, newValues: Partial<Record<string, string | string[]>>) => {
    onSetFilters(newValues as Record<string, string | string[]>);
  }, [onSetFilters]);

  // Manually sync to URL in a single effect
  useEffect(() => {
    setSearchParams(params => {
      const newParams = new URLSearchParams();
      let filtersChanged = false;

      Object.entries(filters).forEach(([key, value]) => {
        const oldValue = params.get(key) ?? undefined;
        
        // Normalize values for comparison
        let newValue: string | undefined;
        const oldNormalized: string | undefined = oldValue;
        
        if (value) {
          if (Array.isArray(value)) {
            newValue = value.join(',');
          } else {
            newValue = value;
          }
          newParams.set(key, newValue);
        } else {
          newValue = undefined;
        }

        // Compare normalized values
        if (oldNormalized !== newValue) {
          filtersChanged = true;
        }
      });

      let sort: string | undefined;

      if (sortBy && direction) {
        sort = `${direction === 'desc' ? '-' : ''}${sortBy}`;
        newParams.set('sort', sort);
      }

      const oldSort = params.get('sort') ?? undefined;

      if (perPage && perPage !== DEFAULT_PAGE_SIZE) {
        newParams.set('page[size]', String(perPage));
      }

      if (!filtersChanged && oldSort === sort) {
        // Only set page cursors if sorting is unchanged
        if (beforeCursor) {
          newParams.set('page[before]', beforeCursor);
        }

        if (afterCursor) {
          newParams.set('page[after]', afterCursor);
        }
      }

      return newParams;
    }, { replace: true }); // Replace current history entry instead of pushing new one
  }, [filters, sortBy, direction, perPage, beforeCursor, afterCursor, setSearchParams]);

  // Define columns
  const columns: DataViewTh[] = useMemo(() => {
    return columnProvider.callback(sortBy, direction, onSort);
  }, [sortBy, direction, onSort, columnProvider]);

  // Determine the active state, errors, and table rows for DataView
  const [ activeState, errors, rows ] = useMemo(() => {
    if (resourceResult.isLoading) {
      return [ DataViewState.loading, undefined, [] ];
    }

    if (resourceResult?.error) {
      const e = resourceResult.error;

      if (e instanceof ApiError) {
        return [ DataViewState.error, e.errors, [] ];
      }

      const errObjects = [{
        title: e.message,
        detail: e.toString(),
      }];

      return [ DataViewState.error, errObjects, [] ];
    }

    if (listResponse?.data && listResponse?.data.length === 0) {
      return [ DataViewState.empty, [], [] ];
    }

    return [
      undefined,
      [],
      listResponse?.data?.map(entry => rowProvider.callback(entry)) ?? []
    ];
  }, [ resourceResult.isLoading, resourceResult.error, listResponse, rowProvider ]);

  useEffect(() => {
    const pageSize = searchParams.get('page[size]');
    const searchFilters: Record<string, string | string[]> = {};

    Object.entries(filters).forEach(([ key, value ]) => {
      if (value) {
        searchFilters[key] = value;
      }
    });

    const modifiedParams: ResourceListParams = {
      filters: searchFilters,
      page: {
        size: pageSize ? Number(pageSize) : DEFAULT_PAGE_SIZE,
        sort: searchParams.get('sort') ?? undefined,
        beforeCursor: searchParams.get('page[before]'),
        afterCursor: searchParams.get('page[after]'),
      }
    };

    onDataViewChange(modifiedParams);
  }, [ searchParams, filters, onDataViewChange ]);

  // Use the same pagination component in the header and footer
  const paginationControl = useMemo(() => {
    return totalCount > 0 ? (
      <Pagination
        itemCount={totalCount}
        perPageOptions={perPageOptions}
        {...pagination}
        isCompact
        onPerPageSelect={handlePerPageSelect}
        onNextClick={handleNextPage}
        onPreviousClick={handlePreviousPage}
      />
    ) : undefined
  }, [totalCount, pagination, handlePerPageSelect, handleNextPage, handlePreviousPage])

  // Define empty state content
  const emptyBody = useMemo(() => {
    const isFiltered = Object.values(filters).some(value => {
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return value?.trim().length > 0
    });

    return (
      <Tbody>
        <Tr key="empty">
          <Td colSpan={columns.length}>
            { errors && errors.length > 0 ? (
              <EmptyState headingLevel="h4" icon={ErrorCircleOIcon} titleText={errors[0].title}>
                <EmptyStateBody>{errors[0].detail}</EmptyStateBody>
              </EmptyState>
            ) : isFiltered ? (
              <EmptyState headingLevel="h4" icon={SearchIcon} titleText={t('common.noResultsFound')}>
                <EmptyStateBody>{t('common.noResultsFoundDescription')}</EmptyStateBody>
              </EmptyState>
            ) : (
              <EmptyState headingLevel="h4" icon={CubesIcon} titleText={t('common.noData')}>
                <EmptyStateBody>{t('common.noDataDescription')}</EmptyStateBody>
              </EmptyState>
            )}
          </Td>
        </Tr>
      </Tbody>
    );
  }, [ errors, filters, columns, t ]);

  const headLoading = useMemo(
    () => <SkeletonTableHead columns={columns} />,
    [columns]
  );

  const bodyLoading = useMemo(
    () => <SkeletonTableBody rowsCount={perPage ?? DEFAULT_PAGE_SIZE} columnsCount={columns.length} />,
    [perPage, columns.length]
  );

  return (
    <DataViewEventsProvider>
      <DataView activeState={activeState}>
        {/* Toolbar with filters and pagination */}
        <DataViewToolbar
          ouiaId={`${ouiaIdPrefix}-toolbar`}
          clearAllFilters={handleClearAllFilters}
          filters={ dataFilters &&
            <DataViewFilters
              onChange={handleFilterChange}
              values={filters}>
              { Object.entries(dataFilters).map(([name, filter]) => {
                if (filter.type === 'checkbox') {
                  return <React.Fragment key={`filter-${name}`} />; // TODO: Add checkbox filter component
                } else {
                  return <TextFilterWrapper
                    key={`filter-${name}`}
                    name={name}
                    filter={filter}
                    pendingFilters={pendingFilters}
                    setPendingFilters={setPendingFilters}
                    onSetFilters={onSetFilters}
                  />;
                }
              })}
            </DataViewFilters>
          }
          pagination={paginationControl}
        />

        {/* Table view */}
        <DataViewTable
          aria-label={ariaLabel}
          variant="compact"
          ouiaId={`${ouiaIdPrefix}-table`}
          columns={columns}
          rows={rows}
          headStates={{
            [DataViewState.loading]: headLoading
          }}
          bodyStates={{
            [DataViewState.error]: emptyBody,
            [DataViewState.empty]: emptyBody,
            [DataViewState.loading]: bodyLoading,
          }}
        />

        {/* Bottom pagination */}
        <DataViewToolbar pagination={paginationControl} />
      </DataView>
    </DataViewEventsProvider>
  );
}
