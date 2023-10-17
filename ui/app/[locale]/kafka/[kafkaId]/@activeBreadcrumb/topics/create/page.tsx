import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";

export default function TopicsActiveBreadcrumb({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  return [
    <BreadcrumbLink
      key={"topics"}
      href={`/kafka/${kafkaId}/topics`}
      showDivider={true}
    >
      Topics
    </BreadcrumbLink>,
    <BreadcrumbItem key={"create-topic"} showDivider={true}>
      Create topic
    </BreadcrumbItem>,
  ];
}
