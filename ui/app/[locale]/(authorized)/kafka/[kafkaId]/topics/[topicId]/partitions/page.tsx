import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/navigation";
import { Suspense } from "react";
import { PartitionsTable } from "./PartitionsTable";

export default function PartitionsPage({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  return (
    <Suspense
      fallback={<PartitionsTable kafkaId={kafkaId} topic={undefined} />}
    >
      <ConnectedPartitions kafkaId={kafkaId} topicId={topicId} />
    </Suspense>
  );
}

async function ConnectedPartitions({ kafkaId, topicId }: KafkaTopicParams) {
  const topic = await getTopic(kafkaId, topicId);
  if (!topic) {
    redirect(`/kafka/${kafkaId}`);
    return null;
  }
  return <PartitionsTable kafkaId={kafkaId} topic={topic} />;
}
