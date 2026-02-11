import { useTranslations } from "next-intl";
import { Suspense } from "react";
import { getConnectorCluster } from "@/api/kafkaConnect/action";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { KafkaConnectorParams } from "../../../kafka-connect/kafkaConnectors.params";
import { ConnectorBreadcrumb } from "./ConnectorBreadcrumb";

export default async function Page({
  params: paramsPromise,
}: {
  params: Promise<KafkaConnectorParams>;
}) {
  const { kafkaId, connectorId } = await paramsPromise;
  return (
    <Suspense
      fallback={<ConnectorActiveBreadcrumb params={{ kafkaId, connectorId }} />}
    >
      <KafkaConnectorActiveBreadcrumb params={{ kafkaId, connectorId }} />
    </Suspense>
  );
}

async function KafkaConnectorActiveBreadcrumb({
  params: { kafkaId, connectorId },
}: {
  params: KafkaConnectorParams;
}) {
  const response = await getConnectorCluster(connectorId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const connectCluster = response.payload!;
  const connectClusterName = connectCluster.data.attributes.name;

  return (
    <ConnectorActiveBreadcrumb
      params={{ kafkaId, connectorId }}
      connectorName={connectClusterName}
    />
  );
}

function ConnectorActiveBreadcrumb({
  params: { kafkaId, connectorId },
  connectorName = "",
}: {
  params: KafkaConnectorParams;
  connectorName?: string;
}) {
  const t = useTranslations();

  return (
    <ConnectorBreadcrumb kafkaId={kafkaId} connectorName={connectorName} />
  );
}
