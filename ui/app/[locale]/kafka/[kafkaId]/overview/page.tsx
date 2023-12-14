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
  return (
    <PageLayout
      clusterOverview={<ConnectedClusterCard data={kpi} />}
      topicsPartitions={<ConnectedTopicsPartitionsCard data={kpi} />}
      clusterCharts={<ConnectedClusterChartsCard data={cluster} />}
      topicCharts={<ConnectedTopicChartsCard data={topic} />}
    />
  );
}

async function ConnectedClusterCard({
  data,
}: {
  data: Promise<{ cluster: ClusterDetail; kpis: ClusterKpis } | null>;
}) {
  const res = await data;
  const brokersTotal = Object.keys(res?.kpis.broker_state || {}).length;
  const brokersOnline =
    Object.values(res?.kpis.broker_state || {}).filter((s) => s === 3).length ||
    0;
  return (
    <ClusterCard
      isLoading={false}
      status={res?.cluster.attributes.status || "n/a"}
      messages={[]}
      name={res?.cluster.attributes.name || "n/a"}
      consumerGroups={0}
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
      partitions={res?.kpis.total_partitions || 0}
      topicsReplicated={topicsTotal - topicsUnderreplicated}
      topicsTotal={topicsTotal}
      topicsUnderReplicated={topicsUnderreplicated}
    />
  );
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
        usedDiskSpace={Object.values(res?.ranges["volumeUsed"] || {})}
        availableDiskSpace={Object.values(res?.ranges["volumeCapacity"] || {})}
        memoryUsage={Object.values(res?.ranges["memory"] || {})}
        cpuUsage={Object.values(res?.ranges["cpu"] || {})}
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
