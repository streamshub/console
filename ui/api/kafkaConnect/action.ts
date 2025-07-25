"use server";

import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import {
  ApiResponse,
  fetchData,
  filterIn,
  filterLike,
  sortParam,
} from "../api";
import { ConnectorsResponse, ConnectorsResponseSchema } from "./schema";

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
    }),
  );

  return fetchData(`/api/connectors`, sp, (rawData) =>
    ConnectorsResponseSchema.parse(rawData),
  );
}
