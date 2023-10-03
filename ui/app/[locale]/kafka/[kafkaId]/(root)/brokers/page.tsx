import { getKafkaCluster } from "@/api/kafka";
import { BrokersTable } from "@/app/[locale]/kafka/[kafkaId]/(root)/brokers/BrokersTable";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";

export default async function BrokersPage({ params }: { params: KafkaParams }) {
  const cluster = await getKafkaCluster(params.kafkaId);
  return (
    <PageSection isFilled>
      <BrokersTable
        brokers={cluster.attributes.nodes}
        controller={cluster.attributes.controller}
      />
    </PageSection>
  );
}
