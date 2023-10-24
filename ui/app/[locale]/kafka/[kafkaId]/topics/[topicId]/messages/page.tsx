import { getTopicMessage, getTopicMessages } from "@/api/messages";
import { getTopic } from "@/api/topics";
import { NoDataEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoDataEmptyState";
import { ConnectedMessagesTable } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/ConnectedMessagesTable";
import {
  MessagesSearchParams,
  parseSearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";

export const revalidate = 0;

export default async function MessagesPage({
  params: { kafkaId, topicId },
  searchParams,
}: {
  params: KafkaTopicParams;
  searchParams: MessagesSearchParams;
}) {
  const topic = await getTopic(kafkaId, topicId);
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

  const messages = await getTopicMessages(kafkaId, topicId, {
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

  switch (true) {
    case messages === null || messages.length === 0:
      return <NoDataEmptyState />;
    default:
      return (
        <ConnectedMessagesTable
          messages={messages}
          selectedMessage={selectedMessage}
          partitions={topic.attributes.partitions.length}
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
