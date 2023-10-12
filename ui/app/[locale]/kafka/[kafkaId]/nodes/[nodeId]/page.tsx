import { KafkaNodeParams } from "@/app/[locale]/kafka/[kafkaId]/nodes/kafkaNode.params";
import { redirect } from "next/navigation";

export default function NodePage({ params }: { params: KafkaNodeParams }) {
  redirect(`${params.nodeId}/configuration`);
}
