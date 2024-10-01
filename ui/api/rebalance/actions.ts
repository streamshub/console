"use server";
import { logger } from "@/utils/logger";
import {
  RebalanceResponse,
  RebalanceResponseSchema,
  RebalanceSchema,
  RebalancesResponse,
  RebalanceStatus,
} from "./schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { getHeaders } from "@/api/api";

const log = logger.child({ module: "rebalance-api" });

export async function getRebalancesList(
  kafkaId: string,
  params: {
    name?: string;
    mode?: string;
    status?: RebalanceStatus[];
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<RebalancesResponse> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[kafkaRebalances]":
        "name,namespace,creationTimestamp,status,mode,brokers",
      "filter[name]": params.name ? `like,*${params.name}*` : undefined,
      "filter[status]":
        params.status && params.status.length > 0
          ? `in,${params.status.join(",")}`
          : undefined,
      "filter[mode]": params.mode ? `like,*${params.mode}*` : undefined,
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: params.sort
        ? (params.sortDir !== "asc" ? "-" : "") + params.sort
        : undefined,
    }),
  );
  const rebalanceQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/rebalances?${rebalanceQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: ["rebalances"],
    },
  });

  log.debug({ url }, "getRebalanceList");
  const rawData = await res.json();
  log.trace({ url, rawData }, "getRebalanceList response");
  return RebalanceResponseSchema.parse(rawData);
}

export async function getRebalanceDetails(
  kafkaId: string,
  rebalanceId: string,
  action?: string,
): Promise<RebalanceResponse | boolean> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/rebalances/${rebalanceId}`;
  const decodedRebalanceId = decodeURIComponent(rebalanceId);
  const body = {
    data: {
      type: "kafkaRebalances",
      id: decodedRebalanceId,
      meta: {
        action: action,
      },
      attributes: {},
    },
  };
  log.debug({ url }, "Fetching rebalance details");
  const res = await fetch(url, {
    headers: await getHeaders(),
    method: "PATCH",
    body: JSON.stringify(body),
  });
  if (action) {
    return res.ok;
  } else {
    const rawData = await res.json();
    return RebalanceSchema.parse(rawData.data);
  }
}
