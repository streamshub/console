import { getConsumerGroups } from "@/api/consumerGroups/actions";
import {
  getKafkaClusterKpis,
  getKafkaClusterMetrics,
  getKafkaTopicMetrics,
} from "@/api/kafka/actions";
import { getTopics, getViewedTopics } from "@/api/topics/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { ConnectedClusterCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedClusterCard";
import { ConnectedClusterChartsCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedClusterChartsCard";
import { ConnectedTopicChartsCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedTopicChartsCard";
import { ConnectedTopicsPartitionsCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedTopicsPartitionsCard";
import { PageLayout } from "@/components/ClusterOverview/PageLayout";
import { ConnectedRecentTopics } from "./ConnectedRecentTopics";

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
  const topics = getTopics(params.kafkaId, { fields: "status", pageSize: 1 });
  const consumerGroups = getConsumerGroups(params.kafkaId, { fields: "state" });
  const viewedTopics = getViewedTopics().then((topics) =>
    topics.filter((t) => t.kafkaId === params.kafkaId),
  );
  return (
    <PageLayout
      clusterOverview={
        <ConnectedClusterCard data={kpi} consumerGroups={consumerGroups} />
      }
      topicsPartitions={<ConnectedTopicsPartitionsCard data={topics} />}
      clusterCharts={<ConnectedClusterChartsCard data={cluster} />}
      topicCharts={<ConnectedTopicChartsCard data={topic} />}
      recentTopics={<ConnectedRecentTopics data={viewedTopics} />}
    />
  );
}
