import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";

export default function PostDeletePage({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  return <RedirectOnLoad url={`/kafka/${kafkaId}/topics`} />;
}
