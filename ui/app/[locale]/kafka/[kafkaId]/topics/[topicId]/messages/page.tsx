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
  };
}) {
  const topic = await getTopic(kafkaId, topicId);
  const limit = stringToInt(searchParams.limit) || 50;
  const partition = stringToInt(searchParams.partition);
  const partitionInfo = topic.attributes.partitions.find(
    (p) => p.partition === partition,
  );
  const offsetMin = partitionInfo?.offsets?.earliest?.offset;
  const offsetMax = partitionInfo?.offsets?.latest?.offset;

  const messages = await getTopicMessages(kafkaId, topicId, {
    pageSize: limit,
    partition,
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
          }}
        />
      );
  }
}
