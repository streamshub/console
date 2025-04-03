"use client";
import {
  BrokerStatus,
  ControllerStatus,
  KafkaNode,
  NodePoolsType,
  NodeRoles,
  Statuses,
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
  nodePool: string[] | undefined;
  sort: NodeListColumn;
  sortDir: "asc" | "desc";
  roles: NodeRoles[] | undefined;
  brokerStatus: BrokerStatus[] | undefined;
  controllerStatus: ControllerStatus[] | undefined;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
  nodePoolList: NodePoolsType | undefined;
  statuses: Statuses | undefined;
};

type State = {
  nodes: KafkaNode[] | undefined;
  perPage: number;
  nodePool: string[] | undefined;
  brokerStatus: BrokerStatus[] | undefined;
  controllerStatus: ControllerStatus[] | undefined;
  sort: NodeListColumn;
  sortDir: "asc" | "desc";
  roles: NodeRoles[] | undefined;
  nodePoolList: NodePoolsType | undefined;
};

export function ConnectedNodesTable({
  nodePool,
  roles,
  nodesCount,
  nodePoolList,
  nodes,
  sort,
  sortDir,
  nextPageCursor,
  prevPageCursor,
  perPage,
  page,
  brokerStatus,
  controllerStatus,
  statuses,
}: ConnectedNodesTableProps) {
  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "nodes">>
  >(
    {
      nodes,
      perPage,
      sort,
      sortDir,
      nodePool,
      roles,
      brokerStatus,
      controllerStatus,
      nodePoolList,
    },

    (state, options) => ({ ...state, ...options, nodes: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { nodes, nodePoolList, ...s } = state;
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
        controllerStatus: undefined,
        brokerStatus: undefined,
      });
    });
  }

  return (
    <NodesTable
      nodePoolList={nodePoolList}
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
      filterBrokerStatus={state.brokerStatus}
      filterControllerStatus={state.controllerStatus}
      onFilterStatusChange={(brokerStatus, controllerStatus) => {
        startTransition(() => {
          updateUrl({ brokerStatus, controllerStatus });
          addOptimistic({ brokerStatus, controllerStatus });
        });
      }}
      nodesCount={nodesCount}
      statuses={statuses}
    />
  );
}
