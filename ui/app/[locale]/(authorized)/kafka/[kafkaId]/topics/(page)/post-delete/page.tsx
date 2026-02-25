import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";

export default async function PostDeletePage({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>;
}) {
  const { kafkaId } = await paramsPromise;
  return <RedirectOnLoad url={`/kafka/${kafkaId}/topics`} />;
}
