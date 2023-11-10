import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaNodeParams } from "@/app/[locale]/kafka/[kafkaId]/nodes/kafkaNode.params";
import { Title } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";

export async function NodeTitle({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const node = cluster.attributes.nodes.find((n) => `${n.id}` === nodeId);
  if (!node) {
    notFound();
  }
  return <Title headingLevel={"h1"}>Node {node.id}</Title>;
}
