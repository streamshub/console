import { getTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { Title } from "@/libs/patternfly/react-core";

export async function TopicName({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
  return <Title headingLevel={"h1"}>{topic.attributes.name}</Title>;
}
