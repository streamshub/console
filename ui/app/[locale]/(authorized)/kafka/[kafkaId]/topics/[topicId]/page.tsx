import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/i18n/routing";

export default function TopicPage({ params }: { params: KafkaTopicParams }) {
  redirect(`/kafka/${params.kafkaId}/topics/${params.topicId}/messages`);
}
