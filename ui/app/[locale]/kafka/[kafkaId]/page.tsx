import { getKafkaCluster } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { redirect } from "@/navigation";
import { notFound } from "next/navigation";

export default async function KafkaRoot({ params }: { params: KafkaParams }) {
  const cluster = await getKafkaCluster(params.kafkaId);
  if (!cluster) {
    notFound();
  }
  redirect(`/kafka/${params.kafkaId}/topics`);
}
