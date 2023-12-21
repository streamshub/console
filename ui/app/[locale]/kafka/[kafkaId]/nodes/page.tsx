import { getKafkaClusterKpis } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import {
  Node,
  NodesTable,
} from "@/app/[locale]/kafka/[kafkaId]/nodes/NodesTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { redirect } from "@/navigation";

export default async function NodesPage({ params }: { params: KafkaParams }) {
  const res = await getKafkaClusterKpis(params.kafkaId);
  if (!res) {
    return redirect("/");
  }
  const { cluster, kpis } = res;
  if (!cluster) {
    redirect("/");
    return null;
  }

  const nodes: Node[] = cluster.attributes.nodes.map((node) => {
    const status = kpis.broker_state[node.id] === 3 ? "Stable" : "Unstable";
    const leaders = kpis.leader_count.byNode[node.id];
    const followers = kpis.replica_count.byNode[node.id] - leaders;
    const diskCapacity = kpis.volume_stats_capacity_bytes.byNode[node.id];
    const diskUsage = kpis.volume_stats_used_bytes.byNode[node.id];
    return {
      id: node.id,
      status,
      hostname: node.host,
      rack: node.rack,
      isLeader: node.id === cluster.attributes.controller.id,
      followers,
      leaders,
      diskCapacity,
      diskUsage,
    };
  });

  return (
    <PageSection isFilled>
      <NodesTable nodes={nodes} />
    </PageSection>
  );
}
