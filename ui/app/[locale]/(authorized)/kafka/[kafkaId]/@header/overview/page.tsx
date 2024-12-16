import { ConnectButton } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/overview/ConnectButton";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";
import { getKafkaCluster } from "@/api/kafka/actions";
import { Suspense } from "react";

export default function Header({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  return (
    <Suspense
      fallback={<OverviewHeader params={{ kafkaId }} managed={false} />}
    >
      <ConnectedHeader params={{ kafkaId }} />
    </Suspense>
  );
}

async function ConnectedHeader({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const cluster = (await getKafkaCluster(kafkaId))?.payload;
  
  return (
    <OverviewHeader
      params={{ kafkaId }}
      managed={cluster?.meta?.managed ?? false}
    />
  );
}

export function OverviewHeader({
  params: { kafkaId },
  managed,
}: {
  params: KafkaParams;
  managed: boolean;
}) {
  const t = useTranslations();
  return (
    <AppHeader
      title={t("ClusterOverview.header")}
      subTitle={t("ClusterOverview.description")}
      actions={[
        <ConnectButton key={"cd"} clusterId={kafkaId} managed={managed} />,
      ]}
    />
  );
}
