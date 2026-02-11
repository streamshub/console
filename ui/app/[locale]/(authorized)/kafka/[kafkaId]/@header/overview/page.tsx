import { ConnectButton } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/overview/ConnectButton";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { getTranslations } from "next-intl/server";
import { getKafkaCluster } from "@/api/kafka/actions";
import { Suspense } from "react";

export default async function Header({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>;
}) {
  const { kafkaId } = await paramsPromise;
  return (
    <Suspense fallback={<OverviewHeader params={{ kafkaId }} />}>
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

  return <OverviewHeader params={{ kafkaId }} />;
}

export async function OverviewHeader({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const t = await getTranslations();

  return (
    <AppHeader
      title={t("ClusterOverview.header")}
      subTitle={t("ClusterOverview.description")}
      actions={[<ConnectButton key={"cd"} clusterId={kafkaId} />]}
    />
  );
}
