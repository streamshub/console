import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { NodesTable } from "@/app/[locale]/kafka/[kafkaId]/nodes/NodesTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { redirect } from "@/navigation";

export default async function NodesPage({ params }: { params: KafkaParams }) {
  const cluster = await getKafkaCluster(params.kafkaId);
  if (!cluster) {
    redirect("/kafka");
    return null;
  }
  return (
    <PageSection isFilled>
      <NodesTable
        nodes={cluster.attributes.nodes}
        controller={cluster.attributes.controller}
        //metrics={cluster.attributes.metrics ?? {}}
      />
    </PageSection>
  );
}
