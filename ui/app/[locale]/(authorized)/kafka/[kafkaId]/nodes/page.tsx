import { getKafkaCluster, getKafkaClusterKpis } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { DistributionChart } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/DistributionChart";
import {
  Node,
  NodesTable,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/NodesTable";
import { Alert, PageSection } from "@/libs/patternfly/react-core";
import { redirect } from "@/navigation";
import { getTranslations } from "next-intl/server";
import { Suspense } from "react";

function nodeMetric(
  metrics: Record<string, number> | undefined,
  nodeId: number,
): number {
  return metrics ? (metrics[nodeId.toString()] ?? 0) : 0;
}

export default function NodesPage({ params }: { params: KafkaParams }) {
  return (
    <Suspense fallback={null}>
      <ConnectedNodes params={params} />
    </Suspense>
  );
}

async function ConnectedNodes({ params }: { params: KafkaParams }) {
  const t = await getTranslations();
  const res = await getKafkaClusterKpis(params.kafkaId);
  let { cluster, kpis } = res || {};

  const nodes: Node[] = (cluster?.attributes.nodes || []).map((node) => {
    const status = kpis
      ? nodeMetric(kpis.broker_state, node.id) === 3
        ? "Stable"
        : "Unstable"
      : "Unknown";
    const leaders = kpis
      ? nodeMetric(kpis.leader_count?.byNode, node.id)
      : undefined;
    const followers =
      kpis && leaders
        ? nodeMetric(kpis.replica_count?.byNode, node.id) - leaders
        : undefined;
    const diskCapacity = kpis
      ? nodeMetric(kpis.volume_stats_capacity_bytes?.byNode, node.id)
      : undefined;
    const diskUsage = kpis
      ? nodeMetric(kpis.volume_stats_used_bytes?.byNode, node.id)
      : undefined;
    return {
      id: node.id,
      status,
      hostname: node.host,
      rack: node.rack,
      isLeader: node.id === cluster?.attributes.controller.id,
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
      {!kpis && (
        <PageSection isFilled={true}>
          <Alert title={t("nodes.kpis_offline")} variant={"warning"} />
        </PageSection>
      )}

      <PageSection isFilled>
        <DistributionChart data={data} />
        <NodesTable nodes={nodes} />
      </PageSection>
    </>
  );
}
