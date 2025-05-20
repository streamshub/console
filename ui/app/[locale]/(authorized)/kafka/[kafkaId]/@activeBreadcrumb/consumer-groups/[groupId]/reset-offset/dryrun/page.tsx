import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export default function DryrunActiveBreadcrumb({
  params: { groupId, kafkaId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  const t = useTranslations();

  return (
    <Breadcrumb>
      <BreadcrumbItem key="home" to="/" showDivider>
        <Tooltip content={t("breadcrumbs.view_all_kafka_clusters")}>
          <HomeIcon />
        </Tooltip>
      </BreadcrumbItem>
      <BreadcrumbItem
        key="overview"
        to={`/kafka/${kafkaId}/overview`}
        showDivider
      >
        {t("breadcrumbs.overview")}
      </BreadcrumbItem>
      <BreadcrumbLink
        key={"cg"}
        href={`/kafka/${kafkaId}/consumer-groups/${groupId}/reset-offset`}
        showDivider={true}
      >
        {t("ConsumerGroupsTable.reset_consumer_offset")}
      </BreadcrumbLink>
      ,
      <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
        {t("ConsumerGroupsTable.dry_run_results_breadcrumb")}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
