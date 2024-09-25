import { ConnectButton } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/overview/ConnectButton";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";
import { getKafkaCluster } from "@/api/kafka/actions";
import { getTopics } from "@/api/topics/actions";
import { Skeleton } from "@patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";

export default function Header({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
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
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  return <OverviewHeader params={{ kafkaId }} />;
}

export function OverviewHeader({
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
