"use server";

import {
  fetchData,
  patchData,
  ApiResponse,
  sortParam,
  filterLike,
} from "@/api/api";
import {
  ClusterDetail,
  ClusterResponse,
  CLustersResponse,
  ClustersResponseSchema,
} from "@/api/kafka/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "kafka-api" });

export async function getKafkaClusters(
  anonymous?: boolean,
  params?: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
    name?: string;
  },
): Promise<ApiResponse<CLustersResponse>> {
  return fetchData(
    "/api/kafkas",
    new URLSearchParams(
      filterUndefinedFromObj({
        "fields[kafkas]": "name,namespace,kafkaVersion",
        "filter[name]": filterLike(params?.name),
        "page[size]": params?.pageSize,
        "page[after]": params?.pageCursor?.startsWith("after:")
          ? params.pageCursor.slice(6)
          : undefined,
        "page[before]": params?.pageCursor?.startsWith("before:")
          ? params.pageCursor.slice(7)
          : undefined,
        sort: sortParam(params?.sort, params?.sortDir),
      }),
    ),
    (rawData: any) => ClustersResponseSchema.parse(rawData),
    anonymous,
    {
      next: {
        revalidate: 60,
      },
    },
  );
}

export async function getKafkaCluster(
  clusterId: string,
  params?: {
    fields?: string;
    duration?: number; // Optional duration
  },
): Promise<ApiResponse<ClusterDetail>> {
  const queryParams = new URLSearchParams({
    "fields[kafkas]":
      params?.fields ??
      "name,namespace,creationTimestamp,status,kafkaVersion,nodes,listeners,metrics,conditions,nodePools,cruiseControlEnabled",
  });

  if (params?.duration) {
    queryParams.append("duration", params.duration.toString());
  }

  return fetchData(
    `/api/kafkas/${clusterId}`,
    queryParams,
    (rawData: any) => ClusterResponse.parse(rawData).data,
    undefined,
    {
      cache: "no-store",
    },
  );
}

export async function updateKafkaCluster(
  clusterId: string,
  reconciliationPaused?: boolean,
): Promise<ApiResponse<undefined>> {
  return patchData(
    `/api/kafkas/${clusterId}`,
    {
      data: {
        type: "kafkas",
        id: clusterId,
        meta: {
          reconciliationPaused: reconciliationPaused,
        },
        attributes: {},
      },
    },
    (_: any) => undefined,
  );
}
