import { getTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";

export async function TopicBreadcrumb({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
  return [
    <BreadcrumbLink
      key={"topics"}
      href={`/kafka/${kafkaId}/topics`}
      showDivider={true}
    >
      Topics
    </BreadcrumbLink>,
    <BreadcrumbItem key={"current-topic"} showDivider={true}>
      {topic.attributes.name}
    </BreadcrumbItem>,
  ];
}
