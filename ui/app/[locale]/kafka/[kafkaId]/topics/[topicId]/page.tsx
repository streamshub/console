import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "next/navigation";

export default function TopicPage({ params }: { params: KafkaTopicParams }) {
  redirect(`${params.topicId}/partitions`);
}
