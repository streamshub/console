import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function NodeBreadcrumb({
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
      Node&nbsp;{nodeId ?? "-"}
    </BreadcrumbItem>,
  ];
}
