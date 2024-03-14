import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";

export const fetchCache = "force-cache";

export async function TopicBreadcrumb({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  return <ConnectedTopicBreadcrumb params={{ kafkaId, topicId }} />;
}

async function ConnectedTopicBreadcrumb({
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
      {topic?.attributes.name}
    </BreadcrumbItem>,
  ];
}
