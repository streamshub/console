import { getTopic, updateTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/navigation";
import { readonly } from "@/utils/runmode";
import { Suspense } from "react";
import { ConfigTable } from "./ConfigTable";

export default function TopicConfiguration({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  return (
    <Suspense
      fallback={<ConfigTable topic={undefined} onSaveProperty={undefined} />}
    >
      <ConnectedTopicConfiguration params={{ kafkaId, topicId }} />
    </Suspense>
  );
}

async function ConnectedTopicConfiguration({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);

  if (!topic) {
    redirect(`/kafka/${kafkaId}`);
    return null;
  }

  async function onSaveProperty(name: string, value: string) {
    "use server";
    if (readonly()) {
      // silently ignore attempt to change a property value in read-only mode
      return true;
    }
    return updateTopic(kafkaId, topicId, undefined, undefined, {
      [name]: {
        value,
      },
    });
  }

  return <ConfigTable topic={topic} onSaveProperty={onSaveProperty} />;
}
