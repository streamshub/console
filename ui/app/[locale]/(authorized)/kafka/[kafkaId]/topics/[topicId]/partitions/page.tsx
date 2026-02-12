import { getTranslations } from "next-intl/server";
import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { Suspense } from "react";
import { PartitionsTable } from "./PartitionsTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `Partitions | ${t("common.title")}`,
  };
}

export default async function PartitionsPage({
  params: paramsPromise,
}: {
  params: Promise<KafkaTopicParams>;
}) {
  const { kafkaId, topicId } = await paramsPromise;
  return (
    <Suspense
      fallback={<PartitionsTable kafkaId={kafkaId} topic={undefined} />}
    >
      <ConnectedPartitions kafkaId={kafkaId} topicId={topicId} />
    </Suspense>
  );
}

async function ConnectedPartitions({ kafkaId, topicId }: KafkaTopicParams) {
  const response = await getTopic(kafkaId, topicId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const topic = response.payload!;
  return <PartitionsTable kafkaId={kafkaId} topic={topic} />;
}
