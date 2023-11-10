"use server";
import { getHeaders } from "@/api/api";
import {
  ClusterDetail,
  ClusterList,
  ClusterResponse,
  ClustersResponseSchema,
} from "@/api/kafka/schema";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "kafka-api" });

export async function getKafkaClusters(): Promise<ClusterList[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,namespace,bootstrapServers`;
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    const rawData = await res.json();
    return ClustersResponseSchema.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaClusters");
    return [];
  }
}

export async function getKafkaCluster(
  clusterId: string,
): Promise<ClusterDetail | null> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}/?fields%5Bkafkas%5D=name,namespace,creationTimestamp,nodes,controller,authorizedOperations,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    const rawData = await res.json();
    return ClusterResponse.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaCluster");
    return null;
  }
}
