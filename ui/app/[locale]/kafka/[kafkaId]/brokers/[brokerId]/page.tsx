import { KafkaBrokerParams } from "@/app/[locale]/kafka/[kafkaId]/brokers/kafkaBroker.params";
import { redirect } from "next/navigation";

export default function BrokerPage({ params }: { params: KafkaBrokerParams }) {
  redirect(`${params.brokerId}/configuration`);
}
