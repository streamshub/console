import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { DistributionChart } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/DistributionChart";
import {
  Node,
  NodesTable,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/NodesTable";
import { Alert, PageSection } from "@/libs/patternfly/react-core";
import { getTranslations } from "next-intl/server";
import { Suspense } from "react";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("nodes.title")} | ${t("common.title")}`,
  };
}

function nodeMetric(
  metrics: { value: string, nodeId: string }[] | undefined,
  nodeId: number,
): number {
  return parseFloat(metrics?.find(e => e.nodeId == nodeId.toString())?.value ?? "0");
}

function nodeRangeMetric(
  metrics: { range: string[][], nodeId?: string }[] | undefined,
  nodeId: number,
): number {
  let range = metrics?.find(e => e.nodeId == nodeId.toString())?.range;
  return parseFloat(range?.[range?.length - 1]?.[1] ?? "0");
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
  const cluster = (await getKafkaCluster(params.kafkaId, {
    fields: 'name,namespace,creationTimestamp,status,kafkaVersion,nodes,controller,authorizedOperations,listeners,conditions,metrics'
  })).payload;
  const metrics = cluster?.attributes.metrics;

  const nodes: Node[] = (cluster?.attributes.nodes ?? []).map((node) => {
    let brokerState = metrics && nodeMetric(metrics.values?.["broker_state"], node.id);
    let status;

    /*
     * https://github.com/apache/kafka/blob/3.8.0/metadata/src/main/java/org/apache/kafka/metadata/BrokerState.java
     */
    switch (brokerState ?? 127) {
      case 0:
        status = "Not Running";
        break;
      case 1:
        status = "Starting";
        break;
      case 2:
        status = "Recovery";
        break;
      case 3:
        status = "Running";
        break;
      case 6:
        status = "Pending Controlled Shutdown";
        break;
      case 7:
        status = "Shutting Down";
        break;
      case 127:
      default:
        status = "Unknown";
        break;
    }

    const leaders = metrics
      ? nodeMetric(metrics.values?.["leader_count"], node.id)
      : undefined;

    const followers =
      metrics && leaders
        ? nodeMetric(metrics.values?.["replica_count"], node.id) - leaders
        : undefined;

    const diskCapacity = metrics
      ? nodeRangeMetric(metrics.ranges?.["volume_stats_capacity_bytes"], node.id)
      : undefined;

    const diskUsage = metrics
      ? nodeRangeMetric(metrics.ranges?.["volume_stats_used_bytes"], node.id)
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
      {!metrics && (
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
