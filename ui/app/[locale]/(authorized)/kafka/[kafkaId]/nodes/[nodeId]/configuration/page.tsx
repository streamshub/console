import { getNodeConfiguration } from "@/api/nodes/actions";
import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export default async function NodeDetails({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  const response = await getNodeConfiguration(kafkaId, nodeId);

  return response.payload ? (
    <PageSection isFilled={true}>
      <ConfigTable config={response.payload} />
    </PageSection>
  ) : <NoDataErrorState errors={response.errors!} />;
}
