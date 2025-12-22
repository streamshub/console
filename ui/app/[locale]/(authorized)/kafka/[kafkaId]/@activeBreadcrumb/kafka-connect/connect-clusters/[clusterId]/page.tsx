import { useTranslations } from "next-intl";

import { KafkaConnectParams } from "../../../../kafka-connect/kafkaConnect.params";
import { Suspense } from "react";
import { getConnectCluster } from "@/api/kafkaConnect/action";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectClusterBreadcrumb } from "./ConnectClusterBreadcrumb";

export default async function Page(
  props: {
    params: Promise<KafkaConnectParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId,
    clusterId
  } = params;

  return (
    <Suspense
      fallback={
        <ConnectClustersActiveBreadcrumb params={{ kafkaId, clusterId }} />
      }
    >
      <KafkaConnectClustersActiveBreadcrumb params={{ kafkaId, clusterId }} />
    </Suspense>
  );
}

async function KafkaConnectClustersActiveBreadcrumb({
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
    <ConnectClustersActiveBreadcrumb
      params={{ kafkaId, clusterId }}
      connectClusterName={connectClusterName}
    />
  );
}

function ConnectClustersActiveBreadcrumb({
  params: { kafkaId, clusterId },
  connectClusterName = "",
}: {
  params: KafkaConnectParams;
  connectClusterName?: string;
}) {
  const t = useTranslations();

  return (
    <ConnectClusterBreadcrumb
      kafkaId={kafkaId}
      connectClusterName={connectClusterName}
    />
  );
}
