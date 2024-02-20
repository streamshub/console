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
import { unstable_noStore as noStore } from "next/cache";
import { cookies } from "next/headers";

export default async function ConnectedMessagesPage({
  params: { kafkaId, topicId },
  searchParams,
}: {
  params: KafkaTopicParams;
  searchParams: MessagesSearchParams;
}) {
  noStore();
  const _ = cookies(); // this is stupid, but required to tell next.js that this action should never be cached
  const topic = await getTopic(kafkaId, topicId);
  if (!topic) {
    redirect(`/kafka/${kafkaId}`);
    return null;
  }
  const {
    limit,
    partition,
    query,
    filter,
    selectedOffset,
    selectedPartition,
    offset,
    timestamp,
    epoch,
  } = parseSearchParams(searchParams);

  async function refresh(): Promise<{ messages: Message[]; ts: Date }> {
    "use server";
    const _ = cookies(); // this is stupid, but required to tell next.js that this action should never be cached
    return await getTopicMessages(kafkaId, topicId, {
      pageSize: limit,
      query,
      partition,
      filter,
      maxValueLength: 150,
    });
  }

  const selectedMessage =
    selectedOffset !== undefined && selectedPartition !== undefined
      ? await getTopicMessage(kafkaId, topicId, {
          offset: selectedOffset,
          partition: selectedPartition,
        })
      : undefined;

  const isFiltered = partition || epoch || offset || timestamp || query;

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
            query,
            "filter[timestamp]": timestamp,
            "filter[epoch]": epoch,
            "filter[offset]": offset,
          }}
          onRefresh={refresh}
        />
      );
  }
}
