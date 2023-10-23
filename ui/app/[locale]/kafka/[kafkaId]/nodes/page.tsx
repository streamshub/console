import { getKafkaCluster } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { NodesTable } from "@/app/[locale]/kafka/[kafkaId]/nodes/NodesTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { redirect } from "next/navigation";

export default async function NodesPage({ params }: { params: KafkaParams }) {
  const cluster = await getKafkaCluster(params.kafkaId);
  if (!cluster) {
    redirect("/kafka");
  }
  return (
    <PageSection isFilled>
      <NodesTable
        nodes={cluster.attributes.nodes}
        controller={cluster.attributes.controller}
      />
    </PageSection>
  );
}
