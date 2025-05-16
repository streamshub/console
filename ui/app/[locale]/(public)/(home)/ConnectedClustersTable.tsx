"use client";

import { ClusterList } from "@/api/kafka/schema";
import { ClustersTable, ClusterTableColumn } from "@/components/ClustersTable";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useTransition } from "react";

export type ConnectedClustersTableProps = {
  clusters: ClusterList[] | undefined;
  clustersCount: number;
  page: number;
  perPage: number;
  sort: ClusterTableColumn;
  sortDir: "asc" | "desc";
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
  authenticated: boolean;
  name: string | undefined;
};

type State = {
  clusters: ClusterList[] | undefined;
  perPage: number;
  sort: ClusterTableColumn;
  sortDir: "asc" | "desc";
  name: string | undefined;
};

export function ConnectedClustersTable({
  clusters,
  clustersCount,
  page,
  perPage,
  nextPageCursor,
  prevPageCursor,
  sort,
  sortDir,
  authenticated,
  name,
}: ConnectedClustersTableProps) {
  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();

  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "kafkas">>
  >(
    {
      clusters,
      perPage,
      sort,
      sortDir,
      name,
    },
    (state, options) => ({ ...state, ...options, clusters: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { clusters, ...s } = state;
    _updateUrl({
      ...s,
      ...newParams,
    });
  };

  function clearFilters() {
    startTransition(() => {
      _updateUrl({});
      addOptimistic({
        name: undefined,
      });
    });
  }

  return (
    <ClustersTable
      clusters={state.clusters}
      authenticated={authenticated}
      page={page}
      perPage={perPage}
      clustersCount={clustersCount}
      onPageChange={(newPage, perPage) => {
        startTransition(() => {
          const pageDiff = newPage - page;
          switch (pageDiff) {
            case -1:
              updateUrl({ perPage, page: prevPageCursor });
              break;
            case 1:
              updateUrl({ perPage, page: nextPageCursor });
              break;
            default:
              updateUrl({ perPage });
              break;
          }
          addOptimistic({ perPage });
        });
      }}
      filterName={state.name}
      onFilterNameChange={(name) => {
        startTransition(() => {
          updateUrl({ name });
          addOptimistic({ name });
        });
      }}
      onClearAllFilters={clearFilters}
    />
  );
}
