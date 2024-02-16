import { getTopicMessage, getTopicMessages } from "@/api/messages/actions";
import { Message } from "@/api/messages/schema";
import { getTopic } from "@/api/topics/actions";
import { NoDataEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoDataEmptyState";
import { ConnectedMessagesTable } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/ConnectedMessagesTable";
import {
  MessagesSearchParams,
  parseSearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/navigation";

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

  async function refresh(): Promise<{ messages: Message[]; ts: Date }> {
    "use server";
    const { messages, ts } = await getTopicMessages(kafkaId, topicId, {
      pageSize: limit,
      partition,
      filter,
      maxValueLength: 150,
    });
    return { messages, ts };
  }

  const selectedMessage =
    selectedOffset !== undefined && selectedPartition !== undefined
      ? await getTopicMessage(kafkaId, topicId, {
          offset: selectedOffset,
          partition: selectedPartition,
        })
      : undefined;

  const isFiltered = partition || epoch || offset || timestamp;

  const { messages, ts } = await refresh();

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
          refresh={refresh}
        />
      );
  }
}
