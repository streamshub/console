import { getBrokerConfiguration } from "@/api/brokers";
import { KafkaBrokerParams } from "@/app/[locale]/kafka/[kafkaId]/brokers/kafkaBroker.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";

export default async function BrokerDetails({
  params: { kafkaId, brokerId },
}: {
  params: KafkaBrokerParams;
}) {
  const config = await getBrokerConfiguration(kafkaId, brokerId);
  return (
    <PageSection isFilled={true}>
      <ConfigTable config={config} />
    </PageSection>
  );
}
