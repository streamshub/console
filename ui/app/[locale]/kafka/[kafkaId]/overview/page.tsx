import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { ConsumerGroupsResponse } from "@/api/consumerGroups/schema";
import {
  ClusterMetric,
  getKafkaClusterKpis,
  getKafkaClusterMetrics,
  getKafkaTopicMetrics,
  TopicMetric,
} from "@/api/kafka/actions";
import { ClusterDetail, ClusterKpis, MetricRange } from "@/api/kafka/schema";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { ClusterCard } from "@/app/[locale]/kafka/[kafkaId]/overview/ClusterCard";
import { ClusterChartsCard } from "@/app/[locale]/kafka/[kafkaId]/overview/ClusterChartsCard";
import { PageLayout } from "@/app/[locale]/kafka/[kafkaId]/overview/PageLayout";
import { TopicChartsCard } from "@/app/[locale]/kafka/[kafkaId]/overview/TopicChartsCard";
import { TopicsPartitionsCard } from "@/app/[locale]/kafka/[kafkaId]/overview/TopicsPartitionsCard";

export default function OverviewPage({ params }: { params: KafkaParams }) {
  const kpi = getKafkaClusterKpis(params.kafkaId);
  const cluster = getKafkaClusterMetrics(params.kafkaId, [
    "volumeUsed",
    "volumeCapacity",
    "memory",
    "cpu",
  ]);
  const topic = getKafkaTopicMetrics(params.kafkaId, [
    "outgoingByteRate",
    "incomingByteRate",
  ]);
  const consumerGroups = getConsumerGroups(params.kafkaId, { fields: "state" });
  return (
    <PageLayout
      clusterOverview={
        <ConnectedClusterCard data={kpi} consumerGroups={consumerGroups} />
      }
      topicsPartitions={<ConnectedTopicsPartitionsCard data={kpi} />}
      clusterCharts={<ConnectedClusterChartsCard data={cluster} />}
      topicCharts={<ConnectedTopicChartsCard data={topic} />}
    />
  );
}

async function ConnectedClusterCard({
  data,
  consumerGroups,
}: {
  data: Promise<{ cluster: ClusterDetail; kpis: ClusterKpis } | null>;
  consumerGroups: Promise<ConsumerGroupsResponse>;
}) {
  const res = await data;
  const groupCount = await consumerGroups.then(
    (grpResp) => grpResp.meta.page.total ?? 0,
  );
  const brokersTotal = Object.keys(res?.kpis.broker_state || {}).length;
  const brokersOnline =
    Object.values(res?.kpis.broker_state || {}).filter((s) => s === 3).length ||
    0;
  const messages = res?.cluster.attributes.conditions
    ?.filter((c) => "Ready" !== c.type)
    .map((c) => ({
      variant:
        c.type === "Error" ? "danger" : ("warning" as "danger" | "warning"),
      subject: {
        type: "cluster" as "cluster" | "broker" | "topic",
        name: res?.cluster.attributes.name ?? "",
        id: res?.cluster.id ?? "",
      },
      message: c.message ?? "",
      date: c.lastTransitionTime ?? "",
    }));

  return (
    <ClusterCard
      isLoading={false}
      status={res?.cluster.attributes.status || "n/a"}
      messages={messages ?? []}
      name={res?.cluster.attributes.name || "n/a"}
      consumerGroups={groupCount}
      brokersOnline={brokersOnline}
      brokersTotal={brokersTotal}
      kafkaVersion={res?.cluster.attributes.kafkaVersion || "n/a"}
    />
  );
}

async function ConnectedTopicsPartitionsCard({
  data,
}: {
  data: Promise<{ cluster: ClusterDetail; kpis: ClusterKpis } | null>;
}) {
  const res = await data;
  const topicsTotal = res?.kpis.total_topics || 0;
  const topicsUnderreplicated = res?.kpis.underreplicated_topics || 0;
  return (
    <TopicsPartitionsCard
      isLoading={false}
      partitions={Math.max(0, res?.kpis.total_partitions || 0)}
      topicsReplicated={Math.max(0, topicsTotal - topicsUnderreplicated)}
      topicsTotal={Math.max(0, topicsTotal)}
      topicsUnderReplicated={Math.max(0, topicsUnderreplicated)}
    />
  );
}

function timeSeriesMetrics(
  ranges: Record<ClusterMetric, MetricRange> | undefined,
  rangeName: ClusterMetric,
): TimeSeriesMetrics[] {
  return ranges
    ? Object.values(ranges[rangeName] ?? {}).map((val) => val ?? {})
    : [];
}

async function ConnectedClusterChartsCard({
  data,
}: {
  data: Promise<{
    cluster: ClusterDetail;
    ranges: Record<ClusterMetric, MetricRange>;
  } | null>;
}) {
  const res = await data;
  return (
    <>
      <ClusterChartsCard
        isLoading={false}
        usedDiskSpace={timeSeriesMetrics(res?.ranges, "volumeUsed")}
        availableDiskSpace={timeSeriesMetrics(res?.ranges, "volumeCapacity")}
        memoryUsage={timeSeriesMetrics(res?.ranges, "memory")}
        cpuUsage={timeSeriesMetrics(res?.ranges, "cpu")}
      />
    </>
  );
}

async function ConnectedTopicChartsCard({
  data,
}: {
  data: Promise<{
    cluster: ClusterDetail;
    ranges: Record<TopicMetric, MetricRange>;
  } | null>;
}) {
  const res = await data;
  return (
    <>
      <TopicChartsCard
        isLoading={false}
        incoming={res?.ranges["incomingByteRate"] || {}}
        outgoing={res?.ranges["outgoingByteRate"] || {}}
      />
    </>
  );
}
