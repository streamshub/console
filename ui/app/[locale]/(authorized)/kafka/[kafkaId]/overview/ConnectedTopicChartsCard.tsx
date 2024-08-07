import { TopicMetric } from "@/api/kafka/actions";
import { ClusterDetail, MetricRange } from "@/api/kafka/schema";
import { TopicChartsCard } from "@/components/ClusterOverview/TopicChartsCard";

export async function ConnectedTopicChartsCard({
  data,
}: {
  data: Promise<{
    cluster: ClusterDetail;
    ranges: Record<TopicMetric, MetricRange> | null;
  } | null>;
}) {
  const res = await data;
  return (
    <TopicChartsCard
      isLoading={false}
      incoming={(res?.ranges && res.ranges["incomingByteRate"]) || {}}
      outgoing={(res?.ranges && res.ranges["outgoingByteRate"]) || {}}
    />
  );
}
