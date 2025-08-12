import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectCluster } from "@/api/kafkaConnect/action";
import { Suspense } from "react";
import { KafkaConnectParams } from "../../../../kafka-connect/kafkaConnect.params";
import { ConnectClusterBreadcrumb } from "./ConnectClusterBreadcrumb";

export default function Page({
  params: { kafkaId, clusterId },
}: {
  params: KafkaConnectParams;
}) {
  return (
    <Suspense fallback={<Header params={{ kafkaId, clusterId }} />}>
      <ConnectClusterAppHeader params={{ kafkaId, clusterId }} />
    </Suspense>
  );
}

async function ConnectClusterAppHeader({
  params: { kafkaId, clusterId },
}: {
  params: KafkaConnectParams;
}) {
  const response = await getConnectCluster(clusterId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const connectCluster = response.payload!;
  const connectClusterName = connectCluster.data.attributes.name;

  return (
    <Header
      params={{ kafkaId, clusterId }}
      connectClusterName={connectClusterName}
    />
  );
}

function Header({
  params: { kafkaId, clusterId },
  connectClusterName = "",
}: {
  params: KafkaConnectParams;
  connectClusterName?: string;
}) {
  const t = useTranslations();

  return (
    <AppHeader
      title={t("KafkaConnect.connect_clusters_title")}
      subTitle={
        <ConnectClusterBreadcrumb
          kafkaId={kafkaId}
          connectClusterName={connectClusterName}
        />
      }
    />
  );
}
