import {
  ClusterDetail,
  ClusterList,
  ClusterResponse,
  ClustersResponse,
} from "@/api/types";
import { getUser } from "@/utils/session";

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
    console.error(err);
    throw err;
  }
}

export async function getKafkaCluster(
  clusterId: string,
): Promise<ClusterDetail> {
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
    console.error(err);
    throw err;
  }
}
