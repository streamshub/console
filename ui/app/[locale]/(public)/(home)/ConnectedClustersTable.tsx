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
};

type State = {
  clusters: ClusterList[] | undefined;
  perPage: number;
  sort: ClusterTableColumn;
  sortDir: "asc" | "desc";
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
    />
  );
}
