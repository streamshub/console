import { getTopicMessage, getTopicMessages } from "@/api/messages/actions";
import { getTopic } from "@/api/topics/actions";
import { NoDataEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoDataEmptyState";
import { ConnectedMessagesTable } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/ConnectedMessagesTable";
import {
  MessagesSearchParams,
  parseSearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/navigation";

export default async function MessagesPage({
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
  const {
    limit,
    partition,
    filter,
    selectedOffset,
    selectedPartition,
    offset,
    timestamp,
    epoch,
  } = parseSearchParams(searchParams);

  const { messages, ts } = await getTopicMessages(kafkaId, topicId, {
    pageSize: limit,
    partition,
    filter,
    maxValueLength: 150,
  });
  const selectedMessage =
    selectedOffset !== undefined && selectedPartition !== undefined
      ? await getTopicMessage(kafkaId, topicId, {
          offset: selectedOffset,
          partition: selectedPartition,
        })
      : undefined;

  const isFiltered = partition || epoch || offset || timestamp;

  switch (true) {
    case !isFiltered && (messages === null || messages.length === 0):
      return <NoDataEmptyState />;
    default:
      return (
        <ConnectedMessagesTable
          messages={messages}
          lastRefresh={ts}
          selectedMessage={selectedMessage}
          partitions={topic.attributes.numPartitions ?? 0}
          params={{
            limit,
            partition,
            selected: searchParams.selected,
            "filter[timestamp]": timestamp,
            "filter[epoch]": epoch,
            "filter[offset]": offset,
          }}
        />
      );
  }
}
