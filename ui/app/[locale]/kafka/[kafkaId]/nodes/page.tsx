import { getKafkaClusterKpis } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { DistributionChart } from "@/app/[locale]/kafka/[kafkaId]/nodes/DistributionChart";
import {
  Node,
  NodesTable,
} from "@/app/[locale]/kafka/[kafkaId]/nodes/NodesTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { redirect } from "@/navigation";

function nodeMetric(metrics: Record<string, number> | undefined, nodeId: number): number {
    return metrics ? (metrics[nodeId.toString()] ?? 0) : 0;
}

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
    const status = nodeMetric(kpis.broker_state, node.id) === 3 ? "Stable" : "Unstable";
    const leaders = nodeMetric(kpis.leader_count?.byNode, node.id);
    const followers = nodeMetric(kpis.replica_count?.byNode, node.id) - leaders;
    const diskCapacity = nodeMetric(kpis.volume_stats_capacity_bytes?.byNode, node.id);
    const diskUsage = nodeMetric(kpis.volume_stats_used_bytes?.byNode, node.id);
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

  const data = Object.fromEntries(
    nodes.map((n) => {
      return [n.id, { followers: n.followers, leaders: n.leaders }];
    }),
  );

  return (
    <>
      <PageSection isFilled>
        <DistributionChart data={data} />
        <NodesTable nodes={nodes} />
      </PageSection>
    </>
  );
}
