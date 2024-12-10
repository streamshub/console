import { getTranslations } from "next-intl/server";
import { getTopicMessage } from "@/api/messages/actions";
import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { redirect } from "@/i18n/routing";
import { ConnectedMessagesTable } from "./ConnectedMessagesTable";
import { MessagesSearchParams, parseSearchParams } from "./parseSearchParams";

export const revalidate = 0;
export const dynamic = "force-dynamic";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("message-browser.title")} | ${t("common.title")}`,
  };
}

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
      topicName={topic.attributes.name!}
      selectedMessage={selectedMessage}
      partitions={topic.attributes.numPartitions ?? 0}
      baseurl={`/kafka/${kafkaId}/topics/${topicId}/messages`}
    />
  );
}
