import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { getConnectCluster } from "@/api/kafkaConnect/action";
import { Suspense } from "react";
import { KafkaConnectParams } from "../../../../kafka-connect/kafkaConnect.params";
import RichText from "@/components/RichText";

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
  connectClusterName = "",
}: {
  params: KafkaConnectParams;
  connectClusterName?: string;
}) {
  const t = useTranslations();

  return (
    <AppHeader
      title={
        decodeURIComponent(connectClusterName) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          connectClusterName
        )
      }
    />
  );
}
