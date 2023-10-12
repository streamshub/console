import { getNodeConfiguration } from "@/api/nodes";
import { KafkaNodeParams } from "@/app/[locale]/kafka/[kafkaId]/nodes/kafkaNode.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";

export default async function NodeDetails({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  const config = await getNodeConfiguration(kafkaId, nodeId);
  return (
    <PageSection isFilled={true}>
      <ConfigTable config={config} />
    </PageSection>
  );
}
