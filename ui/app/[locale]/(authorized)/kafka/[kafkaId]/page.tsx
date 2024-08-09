import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { redirect } from "@/navigation";

export const dynamic = "force-dynamic";

export default function KafkaRoot({ params }: { params: KafkaParams }) {
  redirect(`/kafka/${params.kafkaId}/overview`);
}
