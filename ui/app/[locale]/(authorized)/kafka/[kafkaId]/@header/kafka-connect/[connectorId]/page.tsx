import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";
import { KafkaConnectorParams } from "../../../kafka-connect/kafkaConnectors.params";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectorCluster } from "@/api/kafkaConnect/action";
import { Suspense } from "react";
import RichText from "@/components/RichText";

export default function Page({
  params: { kafkaId, connectorId },
}: {
  params: KafkaConnectorParams;
}) {
  return (
    <Suspense fallback={<Header params={{ kafkaId, connectorId }} />}>
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

  return (
    <Header params={{ kafkaId, connectorId }} connectorName={connectorName} />
  );
}

function Header({
  connectorName = "",
}: {
  params: KafkaConnectorParams;
  connectorName?: string;
}) {
  const t = useTranslations();

  return (
    <AppHeader
      title={
        decodeURIComponent(connectorName) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          connectorName
        )
      }
    />
  );
}
