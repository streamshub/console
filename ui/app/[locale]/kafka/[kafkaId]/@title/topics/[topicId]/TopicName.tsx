import { getTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { Title } from "@/libs/patternfly/react-core";
import { Skeleton } from "@patternfly/react-core";
import { Suspense } from "react";

export const fetchCache = "force-cache";

export function TopicName({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  return (
    <Title headingLevel={"h1"}>
      <Suspense fallback={<Skeleton width="35%" />}>
        <ConnectedTopicName params={{ kafkaId, topicId }} />
      </Suspense>
    </Title>
  );
}

async function ConnectedTopicName({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
  return topic.attributes.name;
}
