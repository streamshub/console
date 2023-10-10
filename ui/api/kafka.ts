import {
  ClusterDetail,
  ClusterList,
  ClusterResponse,
  ClustersResponse,
} from "@/api/types";
import { logger } from "@/utils/logger";
import { getUser } from "@/utils/session";

const log = logger.child({ module: "kafka-api" });

export async function getKafkaClusters(): Promise<ClusterList[]> {
  const user = await getUser();
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${user.accessToken}`,
      },
      cache: "no-store",
    });
    const rawData = await res.json();
    return ClustersResponse.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaClusters");
    return [];
  }
}

export async function getKafkaCluster(
  clusterId: string,
): Promise<ClusterDetail | null> {
  const user = await getUser();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}/?fields%5Bkafkas%5D=name,namespace,creationTimestamp,nodes,controller,authorizedOperations,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${user.accessToken}`,
      },
      cache: "no-store",
    });
    const rawData = await res.json();
    return ClusterResponse.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaCluster");
    return null;
  }
}
