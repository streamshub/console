"use client";

import { KafkaUser } from "@/api/kafkaUsers/schema";
import { KafkaUserColumn, KafkaUsersTable } from "./KafkaUsersTable";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useTransition } from "react";

export type ConnectedKafkaUsersTableProps = {
  kafkaId: string;
  kafkaUsers: KafkaUser[] | undefined;
  kafkaUserCount: number;
  page: number;
  perPage: number;
  username: string | undefined;
  sortDir: "asc" | "desc";
  sort: KafkaUserColumn;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
};

type State = {
  kafkaUsers: KafkaUser[] | undefined;
  perPage: number;
  sort: KafkaUserColumn;
  sortDir: "asc" | "desc";
  username: string | undefined;
};

export function ConnectedKafkaUsersTable({
  kafkaId,
  kafkaUsers,
  kafkaUserCount,
  sort,
  sortDir,
  nextPageCursor,
  prevPageCursor,
  page,
  perPage,
  username,
}: ConnectedKafkaUsersTableProps) {
  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "kafkaUsers">>
  >(
    {
      kafkaUsers,
      perPage,
      sort,
      sortDir,
      username,
    },

    (state, options) => ({ ...state, ...options, kafkaUser: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { kafkaUsers, ...s } = state;
    _updateUrl({
      ...s,
      ...newParams,
    });
  };

  function clearFilters() {
    startTransition(() => {
      _updateUrl({});
      addOptimistic({
        username: undefined,
      });
    });
  }

  return (
    <KafkaUsersTable
      kafkaId={kafkaId}
      kafkaUsers={state.kafkaUsers}
      kafkaUserCount={kafkaUserCount}
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
      onClearAllFilters={clearFilters}
      filterUsername={state.username}
      onFilterUsernameChange={(username) => {
        startTransition(() => {
          updateUrl({ username });
          addOptimistic({ username });
        });
      }}
    />
  );
}
