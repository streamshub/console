import { getTranslations } from "next-intl/server";
import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { getKafkaCluster } from "@/api/kafka/actions";
import { getTopics, getViewedTopics } from "@/api/topics/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { ConnectedClusterCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedClusterCard";
import { ConnectedClusterChartsCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedClusterChartsCard";
import { ConnectedTopicChartsCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedTopicChartsCard";
import { ConnectedTopicsPartitionsCard } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/overview/ConnectedTopicsPartitionsCard";
import { PageLayout } from "@/components/ClusterOverview/PageLayout";
import { ConnectedRecentTopics } from "./ConnectedRecentTopics";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("overview.title")} | ${t("common.title")}`,
  };
}

export default async function OverviewPage({
  params,
}: {
  params: KafkaParams;
}) {
  const kafkaCluster = getKafkaCluster(params.kafkaId, {
    fields:
      "name,namespace,creationTimestamp,status,kafkaVersion,nodes,listeners,conditions,metrics",
  }).then((r) => r.payload ?? null);

  const topics = getTopics(params.kafkaId, { fields: "status", pageSize: 1 });
  const consumerGroups = getConsumerGroups(params.kafkaId, {
    fields: "groupId,state",
  });
  const viewedTopics = getViewedTopics().then((topics) =>
    topics.filter((t) => t.kafkaId === params.kafkaId),
  );

  console.log("kafkaCLuster", kafkaCluster);
  return (
    <PageLayout
      clusterOverview={
        <ConnectedClusterCard
          cluster={kafkaCluster}
          consumerGroups={consumerGroups}
        />
      }
      topicsPartitions={<ConnectedTopicsPartitionsCard data={topics} />}
      clusterCharts={<ConnectedClusterChartsCard cluster={kafkaCluster} />}
      topicCharts={<ConnectedTopicChartsCard cluster={kafkaCluster} />}
      recentTopics={<ConnectedRecentTopics data={viewedTopics} />}
    />
  );
}
