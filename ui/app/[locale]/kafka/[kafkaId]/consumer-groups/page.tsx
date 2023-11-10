import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { notFound } from "next/navigation";
import { ConsumerGroupsTable } from "./ConsumerGroupsTable";

export default async function AsyncConsumerGroupsPage({
  params: { kafkaId },
  searchParams,
}: {
  params: KafkaParams;
  searchParams: {
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  };
}) {
  async function refresh() {
    "use server";
    const res = await getConsumerGroups(kafkaId, searchParams);
    return res.data;
  }

  const consumerGroups = await getConsumerGroups(kafkaId, searchParams);
  if (!consumerGroups) {
    notFound();
  }
  return (
    <ConsumerGroupsTable
      kafkaId={kafkaId}
      page={consumerGroups.meta.page.pageNumber || 1}
      total={consumerGroups.meta.page.total || 0}
      consumerGroups={consumerGroups.data}
      refresh={refresh}
    />
  );
}
