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
  const sp = new URLSearchParams({
    "fields[kafkas]": "name,namespace,kafkaVersion",
    sort: "name",
  });
  const kafkaClustersQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas?${kafkaClustersQuery}`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      next: {
        revalidate: 30,
      },
    });
    const rawData = await res.json();
    log.trace(rawData, "getKafkaClusters response");
    return ClustersResponseSchema.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaClusters");
    throw new Error("getKafkaClusters: couldn't connect with backend");
  }
}

export async function getKafkaCluster(
  clusterId: string,
  params?: {
    fields?: string;
  }
): Promise<ClusterDetail | null> {
  const sp = new URLSearchParams({
    "fields[kafkas]":
      params?.fields ?? "name,namespace,creationTimestamp,status,kafkaVersion,nodes,controller,authorizedOperations,listeners,conditions,nodePools,cruiseControlEnabled",
  });
  const kafkaClusterQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}?${kafkaClusterQuery}`;
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    if (res.status === 200) {
      const rawData = await res.json();
      log.trace(rawData, "getKafkaCluster response");
      return ClusterResponse.parse(rawData).data;
    }
    return null;
  } catch (err) {
    log.error({ err, clusterId }, "getKafkaCluster");
    throw new Error("getKafkaCluster: couldn't connect with backend");
  }
}

export async function updateKafkaCluster(
  clusterId: string,
  reconciliationPaused?: boolean,
): Promise<boolean> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}`;
  const body = {
    data: {
      type: "kafkas",
      id: clusterId,
      meta: {
        reconciliationPaused: reconciliationPaused,
      },
      attributes: {},
    },
  };

  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
      method: "PATCH",
      body: JSON.stringify(body),
    });

    if (res.status === 200) {
      return true;
    } else {
      return false;
    }
  } catch (e) {
    return false;
  }
}
