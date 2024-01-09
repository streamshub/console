import { deleteTopic, getTopic } from "@/api/topics/actions";
import { DeleteTopicModal } from "@/app/[locale]/kafka/[kafkaId]/@modal/topics/[topicId]/delete/DeleteTopicModal";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";

export default async function DeletePage({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const topic = await getTopic(kafkaId, topicId);

  async function onDelete() {
    "use server";
    await deleteTopic(kafkaId, topicId);
  }

  return (
    <DeleteTopicModal
      topicName={topic?.attributes.name || ""}
      onDelete={onDelete}
    />
  );
}
