import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { redirect } from "@/i18n/routing";

//export const dynamic = "force-dynamic";

export default async function KafkaRoot({
  params: paramsPromise
}: {
  params: Promise<KafkaParams>
}) {
  const params = await paramsPromise;
  redirect(`/kafka/${params.kafkaId}/overview`);
}
