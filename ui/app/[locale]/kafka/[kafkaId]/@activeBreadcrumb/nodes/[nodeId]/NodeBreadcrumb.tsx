import { getKafkaCluster } from "@/api/kafka";
import { KafkaNodeParams } from "@/app/[locale]/kafka/[kafkaId]/nodes/kafkaNode.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";

export async function NodeBreadcrumb({
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
  return [
    <BreadcrumbLink
      key={"nodes"}
      href={`/kafka/${kafkaId}/nodes`}
      showDivider={true}
    >
      Nodes
    </BreadcrumbLink>,
    <BreadcrumbItem key={"current-node"} showDivider={true}>
      Node {node.id}
    </BreadcrumbItem>,
  ];
}
