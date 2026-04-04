import { AppHeader } from "@/components/AppHeader";
import { getTranslations } from "next-intl/server";
import { KafkaConnectorParams } from "../../../kafka-connect/kafkaConnectors.params";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectorCluster } from "@/api/kafkaConnect/action";
import { Suspense } from "react";
import RichText from "@/components/RichText";
import { ManagedConnectorLabel } from "../../../kafka-connect/ManagedConnectorLabel";

export default async function Page({
  params: paramsPromise,
}: {
  params: Promise<KafkaConnectorParams>;
}) {
  const { kafkaId, connectorId } = await paramsPromise;
  return (
    <Suspense
      fallback={<Header params={{ kafkaId, connectorId }} managed={false} />}
    >
      <ConnectedAppHeader params={{ kafkaId, connectorId }} />
    </Suspense>
  );
}

async function ConnectedAppHeader({
  params: { kafkaId, connectorId },
}: {
  params: KafkaConnectorParams;
}) {
  const response = await getConnectorCluster(connectorId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const connectCluster = response.payload!;
  const connectorName = connectCluster.data.attributes.name;
  const managed = connectCluster.data.meta.managed;

  return (
    <Header
      params={{ kafkaId, connectorId }}
      connectorName={connectorName}
      managed={managed}
    />
  );
}

async function Header({
  connectorName = "",
  managed,
}: {
  params: KafkaConnectorParams;
  connectorName?: string;
  managed: boolean;
}) {
  const t = await getTranslations();

  return (
    <AppHeader
      title={
        decodeURIComponent(connectorName) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          <>
            {connectorName}
            {managed === true && <ManagedConnectorLabel />}
          </>
        )
      }
    />
  );
}
