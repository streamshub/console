import { getTranslations } from "next-intl/server";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectCluster } from "@/api/kafkaConnect/action";
import { ConnectClusterDetails } from "./ConnectClusterDetails";
import { ConnectorState } from "@/api/kafkaConnect/schema";

export async function generateMetadata(props: {
  params: Promise<{ kafkaId: string; clusterId: string }>;
}) {
  const t = await getTranslations();

  return {
    title: `${t("KafkaConnect.connect_clusters_title")} ${(await props.params).clusterId} | ${t("common.title")}`,
  };
}

export default async function ConnectClusterPage(
  props: {
    params: Promise<{ kafkaId: string; clusterId: string }>;
  }
) {
  const params = await props.params;
  return (
    <PageSection>
      <Suspense
        fallback={
          <ConnectClusterDetails
            kafkaId=""
            connectVersion={""}
            workers={0}
            data={[]}
            plugins={[]}
          />
        }
      >
        <ConnectedConnectClusterDetails
          clusterId={params.clusterId}
          kafkaId={params.kafkaId}
        />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedConnectClusterDetails({
  clusterId,
  kafkaId,
}: {
  clusterId: string;
  kafkaId: string;
}) {
  const response = await getConnectCluster(clusterId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const connectCluster = response.payload!;

  const version = connectCluster.data.attributes.version;
  const workers = connectCluster.data.attributes.replicas;

  const plugins = connectCluster.data.attributes.plugins || [];

  const connectorData = (connectCluster.included ?? []).map((connector) => ({
    id: connector.id,
    managed: connector.meta?.managed || false,
    name: connector.attributes.name,
    type: connector.attributes.type as "source" | "sink",
    state: connector.attributes.state as ConnectorState,
    replicas: workers ?? null,
  }));

  return (
    <ConnectClusterDetails
      kafkaId={kafkaId}
      connectVersion={version ?? ""}
      workers={workers}
      data={connectorData}
      plugins={plugins}
    />
  );
}
