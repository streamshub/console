import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { redirect } from "@/i18n/routing";

export default async function NodePage(props: { params: Promise<KafkaNodeParams> }) {
  const params = await props.params;
  redirect(`/kafka/${params.kafkaId}/nodes/${params.nodeId}/configuration`);
}
