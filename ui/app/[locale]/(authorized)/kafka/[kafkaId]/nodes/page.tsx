import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { DistributionChart } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/DistributionChart";
import {
  Node,
  NodeStatus,
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
): number | undefined {
  let metricValue = metrics?.find(e => e.nodeId == nodeId.toString())?.value;
  return metricValue ? parseFloat(metricValue) : undefined;
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
    let brokerStatus;
    let brokerStable = false;

    if ((node.roles ?? [ "broker" ]).includes("broker")) {
      /*
       * https://github.com/apache/kafka/blob/3.8.0/metadata/src/main/java/org/apache/kafka/metadata/BrokerState.java
       */
      switch (brokerState ?? 127) {
        case 0:
          brokerStatus = "Not Running";
          break;
        case 1:
          brokerStatus = "Starting";
          break;
        case 2:
          brokerStatus = "Recovery";
          break;
        case 3:
          brokerStatus = "Running";
          brokerStable = true;
          break;
        case 6:
          brokerStatus = "Pending Controlled Shutdown";
          break;
        case 7:
          brokerStatus = "Shutting Down";
          break;
        case 127:
        default:
          brokerStatus = "Unknown";
          break;
      }
    }

    let controllerStatus;
    let controllerStable = true;

    if ((node.roles ?? []).includes("controller")) {
      let metadataState = node.metadataState!;
      if (metadataState.status == "leader") {
        controllerStatus = "Quorum Leader";
      } else if (metadataState.lag > 0) {
        controllerStatus = "Quorum Follower (Lagged)";
        controllerStable = false;
      } else {
        controllerStatus = "Quorum Follower";
      }
    }

    const leaders = metrics
      ? nodeMetric(metrics.values?.["leader_count"], node.id) ?? 0
      : undefined;

    const followers =
      metrics && leaders
        ? nodeMetric(metrics.values?.["replica_count"], node.id) ?? 0 - leaders
        : undefined;

    const diskCapacity = metrics
      ? nodeRangeMetric(metrics.ranges?.["volume_stats_capacity_bytes"], node.id)
      : undefined;

    const diskUsage = metrics
      ? nodeRangeMetric(metrics.ranges?.["volume_stats_used_bytes"], node.id)
      : undefined;

    return {
      id: node.id,
      nodePool: node.nodePool ?? "N/A",
      roles: node.roles ?? [ "broker" ],
      isLeader: node.metadataState?.status == "leader",
      brokerStatus: brokerStatus ? {
        stable: brokerStable,
        description: brokerStatus,
      } : undefined,
      controllerStatus: controllerStatus ? {
        stable: controllerStable,
        description: controllerStatus,
      } : undefined,
      hostname: node.host,
      rack: node.rack,
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
