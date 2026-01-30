import { getTranslations } from "next-intl/server";
import { getNodeConfiguration } from "@/api/nodes/actions";
import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata(props: { params: Promise<{ kafkaId: string, nodeId: string  }>}) {
  const t = await getTranslations();

  return {
    title: `Broker ${(await props.params).nodeId} Configuration | ${t("common.title")}`,
  };
}

export default async function NodeDetails(
  props: {
    params: Promise<KafkaNodeParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId,
    nodeId
  } = params;

  const response = await getNodeConfiguration(kafkaId, nodeId);

  return response.payload ? (
    <PageSection isFilled={true}>
      <ConfigTable config={response.payload} />
    </PageSection>
  ) : <NoDataErrorState errors={response.errors!} />;
}
