import { getTranslations } from "next-intl/server";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectorCluster } from "@/api/kafkaConnect/action";
import { ConnectorDetails } from "./ConnectorDetails";

export async function generateMetadata(props: {
  params: Promise<{ kafkaId: string; connectorId: string }>;
}) {
  const t = await getTranslations();

  return {
    title: `${t("KafkaConnect.connect_clusters_title")} ${(await props.params).connectorId} | ${t("common.title")}`,
  };
}

export default async function ConnectClusterPage(
  props: {
    params: Promise<{ kafkaId: string; connectorId: string }>;
  }
) {
  const params = await props.params;
  return (
    <PageSection>
      <Suspense
        fallback={
          <ConnectorDetails
            className={""}
            workerId={""}
            state={"UNASSIGNED"}
            type={"source"}
            topics={[]}
            maxTasks={0}
            connectorTask={[]}
            config={{}}
          />
        }
      >
        <ConnectConnectorDetails connectorId={params.connectorId} />
      </Suspense>
    </PageSection>
  );
}

async function ConnectConnectorDetails({
  connectorId,
}: {
  connectorId: string;
}) {
  const response = await getConnectorCluster(connectorId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const connectCluster = response.payload!;

  const data = connectCluster.data;
  const included = connectCluster.included || [];
  const connectorTasks = included.filter(
    (item) => item.type === "connectorTasks",
  );
  const workerId = data.attributes.workerId ?? "-";

  return (
    <ConnectorDetails
      className={data.attributes.config?.["connector.class"] || ""}
      workerId={workerId}
      state={data.attributes.state}
      type={data.attributes.type || ""}
      topics={data.attributes.topics}
      maxTasks={Number(data.attributes.config?.["tasks.max"] ?? 0)}
      connectorTask={connectorTasks}
      config={data.attributes.config || {}}
    />
  );
}
