import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { Skeleton } from "@patternfly/react-core";
import { Suspense } from "react";
import { useTranslations } from "next-intl";

export async function NodeBreadcrumb({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  const t = useTranslations();
  return [
    <BreadcrumbLink
      key={"nodes"}
      href={`/kafka/${kafkaId}/nodes`}
      showDivider={true}
    >
      {t("nodes.title")}
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
  return (await getKafkaCluster(kafkaId))?.
    payload?.
    attributes.
    nodes.
    find((n) => `${n.id}` === nodeId)?.
    id ?? "-";
}
