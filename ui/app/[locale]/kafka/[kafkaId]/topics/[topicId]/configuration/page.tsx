import { getTopic, updateTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { revalidateTag } from "next/cache";
import { ConfigTable } from "./ConfigTable";

export default async function TopicConfiguration({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);

  async function onSaveProperty(name: string, value: string) {
    "use server";
    const res = await updateTopic(kafkaId, topicId, undefined, undefined, {
      [name]: {
        value,
      },
    });
    if (res === true) {
      revalidateTag(`topic-${topicId}`);
    }
    return res;
  }

  return (
    <PageSection isFilled={true}>
      <ConfigTable topic={topic} onSaveProperty={onSaveProperty} />
    </PageSection>
  );
}
