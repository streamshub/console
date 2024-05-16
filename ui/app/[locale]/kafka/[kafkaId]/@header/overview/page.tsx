import { ConnectButton } from "@/app/[locale]/kafka/[kafkaId]/@header/overview/ConnectButton";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";

export default function OverviewHeader({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const t = useTranslations();
  return (
    <AppHeader
      title={t("ClusterOverview.header")}
      subTitle={t("ClusterOverview.description")}
      actions={[<ConnectButton key={"cd"} clusterId={kafkaId} />]}
    />
  );
}
