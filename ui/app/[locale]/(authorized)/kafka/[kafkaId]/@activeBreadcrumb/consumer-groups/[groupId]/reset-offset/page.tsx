import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import RichText from "@/components/RichText";
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export default function ConsumerGroupsActiveBreadcrumb({
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
        href={`/kafka/${kafkaId}/consumer-groups`}
        showDivider={true}
      >
        {t("breadcrumbs.consumer_groups")}
      </BreadcrumbLink>
      <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
        {decodeURIComponent(groupId) === "+" ? <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText> : groupId}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
