import { getKafkaCluster } from "@/api/kafka/actions";
import { ClusterDetail } from "@/api/kafka/schema";
import { ClusterConnectionDetailsClient } from "./ClusterConnectionDetailsClient";

export async function ClusterConnectionDetails({
  clusterId,
}: {
  clusterId: string;
}) {
  const data = (await getKafkaCluster(clusterId))?.payload;
  if (!data) {
    return null;
  }
  return <ClusterConnectionDetailsClient data={data} />;
}
