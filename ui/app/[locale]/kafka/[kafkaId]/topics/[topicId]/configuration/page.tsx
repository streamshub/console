import { getTopic, updateTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { ConfigTable } from "./ConfigTable";

export default function TopicConfiguration({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  return (
    <PageSection isFilled={true}>
      <Suspense
        fallback={<ConfigTable topic={undefined} onSaveProperty={undefined} />}
      >
        <ConnectedTopicConfiguration params={{ kafkaId, topicId }} />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedTopicConfiguration({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);

  async function onSaveProperty(name: string, value: string) {
    "use server";
    return updateTopic(kafkaId, topicId, undefined, undefined, {
      [name]: {
        value,
      },
    });
  }

  return <ConfigTable topic={topic} onSaveProperty={onSaveProperty} />;
}
