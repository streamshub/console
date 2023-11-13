import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
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
      {groupId}
    </BreadcrumbItem>,
  ];
}
