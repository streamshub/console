import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";

export default function ConsumerGroupsHeader({
  params,
}: {
  params: KafkaParams;
}) {
  return <AppHeader title={"Groups"} />;
}
