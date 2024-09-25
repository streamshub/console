import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { redirect } from "@/i18n/routing";

export default function NodePage({ params }: { params: KafkaNodeParams }) {
  redirect(`/kafka/${params.kafkaId}/nodes/${params.nodeId}/configuration`);
}
