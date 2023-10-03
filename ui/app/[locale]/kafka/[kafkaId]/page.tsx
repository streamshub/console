import { getResource } from "@/api/resources";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { notFound, redirect } from "next/navigation";

export default async function KafkaRoot({ params }: { params: KafkaParams }) {
  const resource = await getResource(params.kafkaId, "kafka");
  if (!resource || !resource.attributes.cluster) {
    notFound();
  }
  redirect(`${params.kafkaId}/brokers`);
}
