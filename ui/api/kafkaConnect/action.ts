"use server";

import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import {
  ApiResponse,
  fetchData,
  filterIn,
  filterLike,
  sortParam,
} from "../api";
import {
  ConnectorsResponse,
  ConnectorsResponseSchema,
  ConnectClustersResponse,
  ConnectClustersResponseSchema,
  ConnectCluster,
  ConnectClusterDetailResponseSchema,
  ConnectorCluster,
  connectorDetailSchema,
} from "./schema";

export async function getKafkaConnectors(params: {
  kafkaId: string;
  name?: string;
  pageSize?: number;
  pageCursor?: string;
  sort?: string;
  sortDir?: string;
}): Promise<ApiResponse<ConnectorsResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "filter[connectCluster.kafkaClusters]": filterIn([params.kafkaId]),
      "filter[name]": filterLike(params.name),
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor?.startsWith("after:")
        ? params.pageCursor.slice(6)
        : undefined,
      "page[before]": params.pageCursor?.startsWith("before:")
        ? params.pageCursor.slice(7)
        : undefined,
      sort: sortParam(params.sort, params.sortDir),

      include: "connectCluster",
      "fields[connectors]": "name,type,state,connectCluster",
    }),
  );

  return fetchData(`/api/connectors`, sp, (rawData) =>
    ConnectorsResponseSchema.parse(rawData),
  );
}

export async function getKafkaConnectClusters(params: {
  kafkaId: string;
  name?: string;
  pageSize?: number;
  pageCursor?: string;
  sort?: string;
  sortDir?: string;
}): Promise<ApiResponse<ConnectClustersResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "filter[kafkaClusters]": filterIn([params.kafkaId]),
      "filter[name]": filterLike(params.name),
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor?.startsWith("after:")
        ? params.pageCursor.slice(6)
        : undefined,
      "page[before]": params.pageCursor?.startsWith("before:")
        ? params.pageCursor.slice(7)
        : undefined,
      sort: sortParam(params.sort, params.sortDir),

      include: "connectors",
      "fields[connects]": "name,version,replicas,connectors",
    }),
  );

  return fetchData(`/api/connects`, sp, (rawData) =>
    ConnectClustersResponseSchema.parse(rawData),
  );
}

export async function getConnectCluster(
  clusterId: string,
): Promise<ApiResponse<ConnectCluster>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      include: "connectors",
      "fields[connects]": "name,version,replicas,connectors,plugins",
    }),
  );
  return fetchData(`/api/connects/${clusterId}`, sp, (rawData) =>
    ConnectClusterDetailResponseSchema.parse(rawData),
  );
}

export async function getConnectorCluster(
  connectorId: string,
): Promise<ApiResponse<ConnectorCluster>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      include: "connectCluster,tasks",
      "fields[connectorTasks]": "taskId,state,workerId,config",
      "fields[connectors]":
        "name,state,type,connectCluster,topics,config,tasks",
    }),
  );

  return fetchData(`/api/connectors/${connectorId}`, sp, (rawData) =>
    connectorDetailSchema.parse(rawData),
  );
}
