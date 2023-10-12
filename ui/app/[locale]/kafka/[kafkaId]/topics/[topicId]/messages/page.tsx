import { getTopicMessages } from "@/api/messages";
import { getTopic } from "@/api/topics";
import { NoDataEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoDataEmptyState";
import { MessagesTable } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/MessagesTable";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { stringToInt } from "@/utils/stringToInt";
import { revalidateTag } from "next/cache";

export default async function Principals({
  params: { kafkaId, topicId },
  searchParams,
}: {
  params: KafkaTopicParams;
  searchParams: {
    limit: string | undefined;
    partition: string | undefined;
    selectedOffset: string | undefined;
    "filter[offset]": string | undefined;
    "filter[ts]": string | undefined;
    "filter[epoch]": string | undefined;
  };
}) {
  const topic = await getTopic(kafkaId, topicId);
  const limit = stringToInt(searchParams.limit) || 50;
  const offset = stringToInt(searchParams["filter[offset]"]);
  const ts = searchParams["filter[ts]"];
  const epoch = stringToInt(searchParams["filter[epoch]"]);
  const selectedOffset = stringToInt(searchParams.selectedOffset);
  const partition = stringToInt(searchParams.partition);
  const partitionInfo = topic.attributes.partitions.find(
    (p) => p.partition === partition,
  );
  const offsetMin = partitionInfo?.offsets?.earliest?.offset;
  const offsetMax = partitionInfo?.offsets?.latest?.offset;

  const timeFilter = ts || epoch;
  const date = timeFilter ? new Date(timeFilter) : undefined;
  const timestamp = date?.toISOString();

  const filter = offset
    ? { type: "offset" as const, value: offset }
    : timestamp
    ? { type: "timestamp" as const, value: timestamp }
    : undefined;

  const messages = await getTopicMessages(kafkaId, topicId, {
    pageSize: limit,
    partition,
    filter,
  });

  switch (true) {
    case messages === null:
      return (
        <NoDataEmptyState
          onRefresh={() => revalidateTag(`messages-${topicId}`)}
        />
      );
    default:
      return (
        <MessagesTable
          messages={messages}
          partitions={topic.attributes.partitions.length}
          offsetMin={offsetMin}
          offsetMax={offsetMax}
          params={{
            limit,
            partition,
            selectedOffset,
            "filter[timestamp]": timestamp,
            "filter[epoch]": epoch,
            "filter[offset]": offset,
          }}
        />
      );
  }
}
