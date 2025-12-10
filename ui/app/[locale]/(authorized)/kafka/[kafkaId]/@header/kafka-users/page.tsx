import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";

export default function KafkaUsersHeader({ params }: { params: KafkaParams }) {
  return <AppHeader title={"Kafka Users"} />;
}
