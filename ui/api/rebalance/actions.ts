"use server";

import { fetchData, patchData, sortParam, filterIn, ApiResponse } from "@/api/api";
import {
  RebalanceMode,
  RebalanceResponse,
  RebalanceResponseSchema,
  RebalanceSchema,
  RebalanceStatus,
  RebalancesResponse,
} from "./schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";

export async function getRebalancesList(
  kafkaId: string,
  params: {
    name?: string;
    mode?: RebalanceMode[];
    status?: RebalanceStatus[];
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ApiResponse<RebalancesResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[kafkaRebalances]":
        "name,namespace,creationTimestamp,status,mode,brokers,optimizationResult,conditions",
      "filter[name]": params.name ? `like,*${params.name}*` : undefined,
      "filter[status]": filterIn(params.status),
      "filter[mode]": filterIn(params.mode),
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: sortParam(params.sort, params.sortDir),
    }),
  );
  return fetchData(
    `/api/kafkas/${kafkaId}/rebalances`,
    sp,
    (rawData) => RebalanceResponseSchema.parse(rawData),
  );
}

export async function getRebalance(
  kafkaId: string,
  rebalanceId: string,
): Promise<ApiResponse<RebalanceResponse>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/rebalances/${rebalanceId}`,
    "",
    (rawData) => RebalanceSchema.parse(rawData.data)
  );
}

export async function patchRebalance(
  kafkaId: string,
  rebalanceId: string,
  action: string,
): Promise<ApiResponse<RebalanceResponse>> {
  return patchData(
    `/api/kafkas/${kafkaId}/rebalances/${rebalanceId}`,
    {
      data: {
        type: "kafkaRebalances",
        id: decodeURIComponent(rebalanceId),
        meta: {
          action: action,
        },
        attributes: {},
      },
    },
    (rawData) => RebalanceSchema.parse(rawData.data)
  );
}
