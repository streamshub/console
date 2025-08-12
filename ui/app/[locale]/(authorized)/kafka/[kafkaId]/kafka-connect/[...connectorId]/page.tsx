import { getTranslations } from "next-intl/server";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectorCluster } from "@/api/kafkaConnect/action";
import { ConnectorDetails } from "./ConnectorDetails";

export async function generateMetadata(props: {
  params: { kafkaId: string; connectorId: string };
}) {
  const t = await getTranslations();

  return {
    title: `${t("KafkaConnect.connect_clusters_title")} ${props.params.connectorId} | ${t("common.title")}`,
  };
}

export default function ConnectClusterPage({
  params,
}: {
  params: { kafkaId: string; connectorId: string };
}) {
  return (
    <PageSection>
      <Suspense
        fallback={
          <ConnectorDetails
            className={""}
            workerId={""}
            state={"UNASSIGNED"}
            type={""}
            topics={[]}
            maxTasks={0}
            connectorTask={[]}
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
  const workerId =
    connectorTasks.length > 0 ? connectorTasks[0].attributes.workerId : "";

  return (
    <ConnectorDetails
      className={data.attributes.config?.["connector.class"] || ""}
      workerId={workerId}
      state={data.attributes.state}
      type={data.attributes.type || ""}
      topics={data.attributes.topics || []}
      maxTasks={Number(data.attributes.config?.["tasks.max"] ?? 0)}
      connectorTask={connectorTasks}
    />
  );
}
