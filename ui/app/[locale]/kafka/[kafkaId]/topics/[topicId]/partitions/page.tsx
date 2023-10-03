import { getTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { notFound } from "next/navigation";
import { TopicDashboard } from "./TopicDashboard";

export default async function AsyncPartitionsPage({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
  if (!topic) {
    notFound();
  }
  return <TopicDashboard kafkaId={kafkaId} topic={topic} />;
}
