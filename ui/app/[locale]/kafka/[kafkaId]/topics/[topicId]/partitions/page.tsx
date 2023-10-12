import { getTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { notFound } from "next/navigation";
import { PartitionsTable } from "./PartitionsTable";

export default async function AsyncPartitionsPage({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
  if (!topic) {
    notFound();
  }
  return <PartitionsTable kafkaId={kafkaId} topic={topic} />;
}
