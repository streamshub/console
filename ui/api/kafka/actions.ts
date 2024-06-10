"use server";

import { fetchData, patchData, ApiResponse, getHeaders } from "@/api/api";
import {
  ClusterDetail,
  ClusterList,
  ClusterResponse,
  ClustersResponseSchema,
} from "@/api/kafka/schema";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "kafka-api" });

export async function getKafkaClusters(anonymous?: boolean): Promise<ApiResponse<ClusterList[]>> {
  return fetchData(
    "/api/kafkas",
    new URLSearchParams({
      "fields[kafkas]": "name,namespace,kafkaVersion",
      sort: "name",
    }),
    (rawData: any) => ClustersResponseSchema.parse(rawData).data,
    anonymous,
    {
      next: {
        revalidate: 60
      }
    },
  );
}

export async function getKafkaCluster(
  clusterId: string,
  params?: {
    fields?: string;
  }
): Promise<ApiResponse<ClusterDetail>> {
  return fetchData(
    `/api/kafkas/${clusterId}`,
    new URLSearchParams({
      "fields[kafkas]": params?.fields ??
        "name,namespace,creationTimestamp,status,kafkaVersion,nodes,controller,listeners,conditions,nodePools,cruiseControlEnabled",
    }),
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
    (_: any) => undefined
  );
}
