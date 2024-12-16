import { deleteTopic, getTopic } from "@/api/topics/actions";
import { DeleteTopicModal } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@modal/topics/[topicId]/delete/DeleteTopicModal";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export default async function DeletePage({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const response = await getTopic(kafkaId, topicId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  async function onDelete() {
    "use server";
    await deleteTopic(kafkaId, topicId);
  }

  const topic = response.payload!;

  return (
    <DeleteTopicModal
      topicName={topic?.attributes.name ?? ""}
      onDelete={onDelete}
    />
  );
}
