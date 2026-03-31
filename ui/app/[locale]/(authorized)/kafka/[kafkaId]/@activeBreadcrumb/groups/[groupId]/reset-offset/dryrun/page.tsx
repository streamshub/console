import { getConsumerGroup } from "@/api/groups/actions";
import { GroupParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/Group.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import RichText from "@/components/RichText";
import { NoDataErrorState } from "@/components/NoDataErrorState";

import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { getTranslations } from "next-intl/server";

export default async function DryrunActiveBreadcrumb({
  params: paramsPromise,
}: {
  params: Promise<GroupParams>;
}) {
  const { groupId, kafkaId } = await paramsPromise;
  const t = await getTranslations();
  const consumerGroup = (await getConsumerGroup(kafkaId, groupId));

  if (consumerGroup.errors) {
    return <NoDataErrorState errors={consumerGroup.errors} />;
  }

  const groupIdDisplay = consumerGroup.payload?.attributes.groupId!;

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
        key={"groups-link-crumb"}
        href={`/kafka/${kafkaId}/groups`}
        showDivider={true}
      >
        {t("breadcrumbs.consumer_groups")}
      </BreadcrumbLink>
      <BreadcrumbItem key={"group-crumb"} showDivider={true} isActive={true}>
        {groupIdDisplay === "" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          groupIdDisplay
        )}
      </BreadcrumbItem>

      <BreadcrumbLink
        key={"group-reset-link-crumb"}
        href={`/kafka/${kafkaId}/groups/${groupId}/reset-offset`}
        showDivider={true}
      >
        {t("GroupsTable.reset_consumer_offset")}
      </BreadcrumbLink>
      <BreadcrumbItem key={"group-reset-dryrun"} showDivider={true} isActive={true}>
        {t("GroupsTable.dry_run_results_breadcrumb")}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
