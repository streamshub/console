import { KafkaNodeParams } from "@/app/[locale]/kafka/[kafkaId]/nodes/kafkaNode.params";
import { redirect } from "@/navigation";

export default function NodePage({ params }: { params: KafkaNodeParams }) {
  redirect(`${params.nodeId}/configuration`);
}
