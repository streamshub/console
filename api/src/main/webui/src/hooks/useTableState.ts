/**
 * Table State Hook - Pagination & Sorting
 *
 * Manages cursor-based pagination and sorting state for API responses.
 * Extracts cursors from API link URLs and handles sort state with automatic
 * pagination reset when sorting changes.
 *
 * This hook is completely self-contained and manages all state internally.
 * Call it BEFORE your data fetching hook and pass its state to your query.
 */

import { useState, useCallback } from 'react';
import type { ThProps } from '@patternfly/react-table';

interface PaginationLinks {
  next?: string | null;
  prev?: string | null;
  first?: string | null;
  last?: string | null;
}

interface UseTableStateOptions<T extends string> {
  initialSortColumn?: T;
  initialSortDirection?: 'asc' | 'desc';
}

interface UseTableStateReturn<T extends string> {
  // Pagination state to pass to your query
  pageCursor: string | undefined;
  pageSize: number | undefined;
  
  // Sorting state to pass to your query
  sortBy: T;
  sortDirection: 'asc' | 'desc';
  
  // Pagination handlers for Pagination component
  handleNextPage: () => void;
  handlePrevPage: () => void;
  handlePerPageChange: (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number
  ) => void;
  resetPagination: () => void;
  
  // Sorting handlers
  handleSort: (column: T) => void;
  getSortParams: (column: T) => ThProps['sort'];
  
  // Method to update with API response data
  setData: (data?: { links?: PaginationLinks }) => void;
}

/**
 * Hook for managing table pagination and sorting
 *
 * @param options - Configuration options
 * @returns Table state and handlers
 *
 * @example
 * ```tsx
 * // Define sortable columns
 * type SortableColumn = 'name' | 'size';
 *
 * // Call hook BEFORE data fetching
 * const table = useTableState<SortableColumn>({
 *   initialSortColumn: 'name',
 *   initialSortDirection: 'asc',
 * });
 *
 * // Pass state to query
 * const { data } = useTopics(kafkaId, {
 *   pageSize: table.pageSize,
 *   pageCursor: table.pageCursor,
 *   sort: table.sortBy,
 *   sortDir: table.sortDirection,
 * });
 *
 * // Update with response data
 * table.setData(data);
 *
 * // Use in table header
 * <Th sort={table.getSortParams('name')}>Name</Th>
 *
 * // Use in Pagination component
 * <Pagination
 *   onNextClick={table.handleNextPage}
 *   onPreviousClick={table.handlePrevPage}
 *   onPerPageSelect={table.handlePerPageChange}
 * />
 * ```
 */
export function useTableState<T extends string>(
  options: UseTableStateOptions<T> = {}
): UseTableStateReturn<T> {
  const {
    initialSortColumn,
    initialSortDirection = 'asc',
  } = options;
  
  // Pagination state
  const [pageSize, setPageSize] = useState<number | undefined>(undefined);
  const [pageCursor, setPageCursor] = useState<string | undefined>(undefined);
  const [data, setDataInternal] = useState<{ links?: PaginationLinks } | undefined>();
  
  // Sorting state
  const [sortBy, setSortBy] = useState<T>(initialSortColumn as T);
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>(initialSortDirection);

  /**
   * Navigate to next page by extracting cursor from next link
   */
  const handleNextPage = useCallback(() => {
    if (data?.links?.next) {
      const url = new URL(data.links.next, window.location.origin);
      const afterCursor = url.searchParams.get('page[after]');
      if (afterCursor) {
        setPageCursor(`after:${afterCursor}`);
      }
    }
  }, [data?.links?.next]);

  /**
   * Navigate to previous page by extracting cursor from prev link
   */
  const handlePrevPage = useCallback(() => {
    if (data?.links?.prev) {
      const url = new URL(data.links.prev, window.location.origin);
      const beforeCursor = url.searchParams.get('page[before]');
      const afterCursor = url.searchParams.get('page[after]');
      
      if (beforeCursor) {
        setPageCursor(`before:${beforeCursor}`);
      } else if (afterCursor) {
        setPageCursor(`after:${afterCursor}`);
      } else {
        // No cursor means we're going back to the first page
        setPageCursor(undefined);
      }
    }
  }, [data?.links?.prev]);

  /**
   * Change page size and reset to first page
   */
  const handlePerPageChange = useCallback((
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number
  ) => {
    setPageSize(newPerPage);
    setPageCursor(undefined);
  }, []);

  /**
   * Reset pagination to first page (useful when filters change)
   */
  const resetPagination = useCallback(() => {
    setPageCursor(undefined);
  }, []);

  /**
   * Handle column sort - toggles direction if same column, resets pagination
   */
  const handleSort = useCallback((column: T) => {
    if (sortBy === column) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
    // Always reset to first page when sorting changes
    setPageCursor(undefined);
  }, [sortBy]);

  /**
   * Get sort params for PatternFly table header
   */
  const getSortParams = useCallback((column: T): ThProps['sort'] => ({
    sortBy: {
      index: sortBy === column ? 0 : undefined,
      direction: sortDirection,
      defaultDirection: 'asc',
    },
    onSort: () => handleSort(column),
    columnIndex: 0,
  }), [sortBy, sortDirection, handleSort]);

  return {
    pageCursor,
    pageSize,
    sortBy,
    sortDirection,
    handleNextPage,
    handlePrevPage,
    handlePerPageChange,
    resetPagination,
    handleSort,
    getSortParams,
    setData: setDataInternal,
  };
}