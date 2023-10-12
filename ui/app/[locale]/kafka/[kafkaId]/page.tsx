import { getKafkaCluster } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { notFound, redirect } from "next/navigation";

export default async function KafkaRoot({ params }: { params: KafkaParams }) {
  const cluster = await getKafkaCluster(params.kafkaId);
  if (!cluster) {
    notFound();
  }
  redirect(`${params.kafkaId}/topics`);
}
