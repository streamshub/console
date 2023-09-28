import { getResource } from "@/api/resources";
import { notFound, redirect } from "next/navigation";

export default async function KafkaRoot({
  params,
}: {
  params: { kafkaId: string };
}) {
  const cluster = await getResource(params.kafkaId, "kafka");
  if (!cluster || !cluster.attributes.cluster) {
    notFound();
  }
  redirect(`${params.kafkaId}/overview`);
}
