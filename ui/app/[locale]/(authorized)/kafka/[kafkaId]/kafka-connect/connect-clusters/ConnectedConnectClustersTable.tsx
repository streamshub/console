"use client";

import { ConnectClusters } from "@/api/kafkaConnect/schema";
import {
  ConnectClustersTable,
  ConnectClustersTableColumn,
} from "./ConnectClustersTable";
import { useRouter } from "@/i18n/routing";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useTransition } from "react";

export type ConnectedConnectClustersTableProps = {
  connectClusters: ConnectClusters[] | undefined;
  connectClustersCount: number;
  page: number;
  perPage: number;
  sort: ConnectClustersTableColumn;
  name: string | undefined;
  sortDir: "asc" | "desc";
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
};

type State = {
  name: string | undefined;
  connectClusters: ConnectClusters[] | undefined;
  perPage: number;
  sort: ConnectClustersTableColumn;
  sortDir: "asc" | "desc";
};

export function ConnectedConnectClustersTable({
  connectClusters,
  connectClustersCount,
  page,
  perPage,
  name,
  sort,
  sortDir,
  nextPageCursor,
  prevPageCursor,
}: ConnectedConnectClustersTableProps) {
  const router = useRouter();

  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "ConsumerGroups">>
  >(
    {
      connectClusters,
      name,
      perPage,
      sort,
      sortDir,
    },
    (state, options) => ({ ...state, ...options, connectClusters: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { connectClusters, ...s } = state;
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
    <ConnectClustersTable
      connectClusters={state.connectClusters}
      page={page}
      perPage={state.perPage}
      total={connectClustersCount}
      onClearAllFilters={clearFilters}
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
    />
  );
}
