import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default function DryrunActiveBreadcrumb({
  params: { groupId, kafkaId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  const t = useTranslations("ConsumerGroupsTable");

  return [
    <BreadcrumbLink
      key={"cg"}
      href={`/kafka/${kafkaId}/consumer-groups/${groupId}/reset-offset`}
      showDivider={true}
    >
      {t("reset_consumer_offset")}
    </BreadcrumbLink>,
    <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
      {t("dry_run_results_breadcrumb")}
    </BreadcrumbItem>,
  ];
}
