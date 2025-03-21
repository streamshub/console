"use client";
import {
  BrokerStatus,
  ControllerStatus,
  KafkaNode,
  NodeListResponse,
  NodePools,
  NodeRoles,
} from "@/api/nodes/schema";
import { NodeListColumn } from "./NodesTable";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useTransition } from "react";
import { NodesTable } from "./NodesTable";

export type ConnectedNodesTableProps = {
  nodes: KafkaNode[] | undefined;
  nodesCount: number;
  page: number;
  perPage: number;
  nodePool: NodePools[] | undefined;
  sort: NodeListColumn;
  sortDir: "asc" | "desc";
  roles: NodeRoles[] | undefined;
  status: (BrokerStatus | ControllerStatus)[] | undefined;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
};

type State = {
  nodes: KafkaNode[] | undefined;
  perPage: number;
  nodePool: NodePools[] | undefined;
  status: (BrokerStatus | ControllerStatus)[] | undefined;
  sort: NodeListColumn;
  sortDir: "asc" | "desc";
  roles: NodeRoles[] | undefined;
};

export function ConnectedNodesTable({
  nodePool,
  roles,
  nodesCount,
  nodes,
  sort,
  sortDir,
  nextPageCursor,
  prevPageCursor,
  perPage,
  page,
  status,
}: ConnectedNodesTableProps) {
  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "topics">>
  >(
    {
      nodes,
      perPage,
      sort,
      sortDir,
      nodePool,
      roles,
      status,
    },
    (state, options) => ({ ...state, ...options, nodes: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { nodes, ...s } = state;
    _updateUrl({
      ...s,
      ...newParams,
    });
  };

  function clearFilters() {
    startTransition(() => {
      _updateUrl({});
      addOptimistic({
        nodePool: undefined,
        roles: undefined,
        status: undefined,
      });
    });
  }

  return (
    <NodesTable
      nodeList={state.nodes}
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
      filterNodePool={state.nodePool}
      filterRole={state.roles}
      onFilterNodePoolChange={(nodePool) => {
        startTransition(() => {
          updateUrl({ nodePool });
          addOptimistic({ nodePool });
        });
      }}
      onClearAllFilters={clearFilters}
      onFilterRoleChange={(roles) => {
        startTransition(() => {
          updateUrl({ roles });
          addOptimistic({ roles });
        });
      }}
      filterStatus={state.status}
      onFilterStatusChange={(status) => {
        startTransition(() => {
          updateUrl({ status });
          addOptimistic({ status });
        });
      }}
    />
  );
}
