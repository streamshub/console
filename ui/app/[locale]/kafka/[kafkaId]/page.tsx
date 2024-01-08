import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { redirect } from "@/navigation";

export default function KafkaRoot({ params }: { params: KafkaParams }) {
  redirect(`/kafka/${params.kafkaId}/overview`);
}
