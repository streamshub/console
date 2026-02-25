import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/i18n/routing";

export default async function TopicPage({
  params: paramsPromise
}: {
  params: Promise<KafkaTopicParams>
}) {
  const params = await paramsPromise;
  redirect(`/kafka/${params.kafkaId}/topics/${params.topicId}/messages`);
}
