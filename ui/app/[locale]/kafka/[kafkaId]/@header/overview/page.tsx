import { ConnectButton } from "@/app/[locale]/kafka/[kafkaId]/@header/overview/ConnectButton";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";

export default function OverviewHeader({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  return (
    <AppHeader
      title={"Cluster overview"}
      subTitle={
        "Key performance indicators and important information regarding the Kafka cluster."
      }
      actions={[<ConnectButton key={"cd"} clusterId={kafkaId} />]}
    />
  );
}
