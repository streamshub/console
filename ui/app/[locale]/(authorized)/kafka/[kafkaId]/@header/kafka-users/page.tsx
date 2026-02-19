import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";

export default async function KafkaUsersHeader({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>;
}) {
  const params = await paramsPromise;
  return <AppHeader title={"Kafka Users"} />;
}
