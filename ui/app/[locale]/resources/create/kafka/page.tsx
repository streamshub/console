import { getKafkaClusters } from "@/api/kafka";
import { Step1 } from "./Step1";

export default async function AsyncCreateResourceStep1Page() {
  const clusters = await getKafkaClusters();
  return (
    <Step1 clusters={clusters.map((c) => c.attributes.bootstrapServers)} />
  );
}
