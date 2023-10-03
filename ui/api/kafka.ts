import {
  ClusterDetail,
  ClusterList,
  ClusterResponse,
  ClustersResponse,
} from "@/api/types";

export async function getKafkaClusters(): Promise<ClusterList[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
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
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}/?fields%5Bkafkas%5D=name,namespace,creationTimestamp,nodes,controller,authorizedOperations,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
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
