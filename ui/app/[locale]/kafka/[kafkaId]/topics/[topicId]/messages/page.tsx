import { getTopicMessage } from "@/api/messages/actions";
import { getTopic } from "@/api/topics/actions";
import { ConnectedMessagesTable } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/ConnectedMessagesTable";
import {
  MessagesSearchParams,
  parseSearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/navigation";

export const revalidate = 0;
export const dynamic = "force-dynamic";

export default async function ConnectedMessagesPage({
  params: { kafkaId, topicId },
  searchParams,
}: {
  params: KafkaTopicParams;
  searchParams: MessagesSearchParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
  if (!topic) {
    redirect(`/kafka/${kafkaId}`);
    return null;
  }
  const { selectedOffset, selectedPartition } = parseSearchParams(searchParams);

  const selectedMessage =
    selectedOffset !== undefined && selectedPartition !== undefined
      ? await getTopicMessage(kafkaId, topicId, {
          offset: selectedOffset,
          partition: selectedPartition,
        })
      : undefined;

  return (
    <ConnectedMessagesTable
      kafkaId={kafkaId}
      topicId={topicId}
      selectedMessage={selectedMessage}
      partitions={topic.attributes.numPartitions ?? 0}
    />
  );
}
