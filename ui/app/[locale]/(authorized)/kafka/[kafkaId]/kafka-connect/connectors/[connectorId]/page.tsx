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
    title: `${t("KafkaConnect.connectors_title")} ${props.params.connectorId} | ${t("common.title")}`,
  };
}

export default function ConnectorDetailsPage({
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
            workerId={0}
            state={"UNASSIGNED"}
            type={""}
            topics={[]}
            maxTasks={0}
            connectorTask={[]}
          />
        }
      >
        <ConnectedConnectorDetails connectorId={params.connectorId} />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedConnectorDetails({
  connectorId,
}: {
  connectorId: string;
}) {
  const response = await getConnectorCluster(connectorId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const connectCluster = response.payload!;

  console.log("connector", connectCluster);

  return (
    <ConnectorDetails
      className={""}
      workerId={0}
      state={"UNASSIGNED"}
      type={""}
      topics={[]}
      maxTasks={0}
      connectorTask={[]}
    />
  );
}
