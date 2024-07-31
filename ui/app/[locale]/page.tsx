import { getKafkaClusters } from "@/api/kafka/actions";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";

export default function Page({}) {
  return <DefaultCluster />;
}

async function DefaultCluster() {
  const clusters = await getKafkaClusters();
  const defaultCluster = clusters[0];
  if (defaultCluster) {
    return <RedirectOnLoad url={`/kafka/${defaultCluster.id}`} />;
  }
}
