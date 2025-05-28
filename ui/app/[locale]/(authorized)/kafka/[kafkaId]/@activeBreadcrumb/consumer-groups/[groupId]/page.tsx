import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import RichText from "@/components/RichText";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default function ConsumerGroupsActiveBreadcrumb({
  params: { groupId, kafkaId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  const t = useTranslations();

  return [
    <BreadcrumbLink
      key={"cg"}
      href={`/kafka/${kafkaId}/consumer-groups`}
      showDivider={true}
    >
      Consumer groups
    </BreadcrumbLink>,
    <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
      {decodeURIComponent(groupId) === "+" ? (
        <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
      ) : (
        groupId
      )}
    </BreadcrumbItem>,
  ];
}
