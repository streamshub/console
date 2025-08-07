"use client";

import { EnrichedConnector } from "@/api/kafkaConnect/schema";
import { ConnectorsTable, ConnectorsTableColumn } from "./ConnectorsTable";
import { useRouter } from "@/i18n/routing";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useTransition } from "react";

export type ConnectedConnectorsTableProps = {
  connectors: EnrichedConnector[] | undefined;
  connectorsCount: number;
  page: number;
  perPage: number;
  sort: ConnectorsTableColumn;
  name: string | undefined;
  sortDir: "asc" | "desc";
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
  kafkaId: string;
};

type State = {
  name: string | undefined;
  connectors: EnrichedConnector[] | undefined;
  perPage: number;
  sort: ConnectorsTableColumn;
  sortDir: "asc" | "desc";
};

export function ConnectedConnectorsTable({
  connectors,
  connectorsCount,
  page,
  perPage,
  name,
  sort,
  sortDir,
  nextPageCursor,
  prevPageCursor,
  kafkaId,
}: ConnectedConnectorsTableProps) {
  const router = useRouter();

  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "connectors">>
  >(
    {
      connectors,
      name,
      perPage,
      sort,
      sortDir,
    },
    (state, options) => ({ ...state, ...options, connectors: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { connectors, ...s } = state;
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
    <ConnectorsTable
      kafkaId={kafkaId}
      connectors={state.connectors}
      page={page}
      perPage={state.perPage}
      total={connectorsCount}
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
