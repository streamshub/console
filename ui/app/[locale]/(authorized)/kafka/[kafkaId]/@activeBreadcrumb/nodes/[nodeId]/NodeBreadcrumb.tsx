import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { Skeleton } from "@patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";

export const fetchCache = "force-cache";

export async function NodeBreadcrumb({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  return [
    <BreadcrumbLink
      key={"nodes"}
      href={`/kafka/${kafkaId}/nodes`}
      showDivider={true}
    >
      Brokers
    </BreadcrumbLink>,
    <BreadcrumbItem key={"current-node"} showDivider={true}>
      Broker&nbsp;
      <Suspense fallback={<Skeleton width="35%" />}>
        <ConnectedNodeBreadcrumb params={{ kafkaId, nodeId }} />
      </Suspense>
    </BreadcrumbItem>,
  ];
}

async function ConnectedNodeBreadcrumb({
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
  return node.id;
}
