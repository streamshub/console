import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";

export default function ConsumerGroupsActiveBreadcrumb({
  params: { groupId, kafkaId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  return [
    <BreadcrumbLink
      key={"cg"}
      href={`/kafka/${kafkaId}/consumer-groups`}
      showDivider={true}
    >
      Consumer groups
    </BreadcrumbLink>,
    <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
      {decodeURIComponent(groupId) === "+" ? <i>Empty Name</i> : groupId}
    </BreadcrumbItem>,
  ];
}
