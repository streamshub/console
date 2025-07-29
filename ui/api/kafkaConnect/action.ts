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
      "filter[connectCluster.kafkaClusters]": filterLike(params.kafkaId),
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

  return fetchData(`/api/connectors`, sp, (rawData) => {
    console.log("Raw API response:", rawData);
    return ConnectorsResponseSchema.parse(rawData);
  });
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
      "filter[kafkaClusters]": filterLike(params.kafkaId),
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
      "fields[connects]": "name,version,connectors",
    }),
  );

  return fetchData(`/api/connects`, sp, (rawData) => {
    console.log("Raw API response:", rawData);
    return ConnectClustersResponseSchema.parse(rawData);
  });
}
