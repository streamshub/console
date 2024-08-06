import { getKafkaClusters } from "@/api/kafka/actions";
import { ClusterSelector } from "./ClusterSelector";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";

export default function Page({}) {
  return <AsyncClusterSelector />;
}

async function AsyncClusterSelector() {
  const clusters = await getKafkaClusters();
  console.log(clusters);
  if (clusters.length === 1) {
    const defaultCluster = clusters[0];
    return <RedirectOnLoad url={`/kafka/${defaultCluster.id}`} />;
  } else {
    return <ClusterSelector clusters={clusters} />;
  }
}
