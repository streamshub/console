"use client";

import { useOptimistic, useTransition } from "react";
import { useFilterParams } from "@/utils/useFilterParams";
import { ClusterList } from "@/api/kafka/schema";
import { ClusterColumn, ClustersTable } from "@/components/ClustersTable";

export type ConnectedClusterTableProps = {
  clusters: ClusterList[] | undefined;
  clusterCount: number;
  page: number;
  perPage: number;
  sort: ClusterColumn;
  sortDir: "asc" | "desc";
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
  authenticated: boolean;
};

type State = {
  clusters: ClusterList[] | undefined;
  perPage: number;
  sort: ClusterColumn;
  sortDir: "asc" | "desc";
};

export function ConnectedClusterTable({
  clusters,
  clusterCount,
  page,
  perPage,
  sort,
  sortDir,
  nextPageCursor,
  prevPageCursor,
  authenticated,
}: ConnectedClusterTableProps) {
  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<State, Partial<State>>(
    { clusters, perPage, sort, sortDir },
    (state, updates) => ({ ...state, ...updates, clusters: undefined }),
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
      clusterCount={clusterCount}
      clusters={state.clusters}
      authenticated={authenticated}
      page={page}
      perPage={state.perPage}
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
