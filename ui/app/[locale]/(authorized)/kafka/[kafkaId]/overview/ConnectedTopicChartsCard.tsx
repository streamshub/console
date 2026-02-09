import { ApiResponse } from "@/api/api";
import { ClusterDetail } from "@/api/kafka/schema";
import { TopicsResponse } from "@/api/topics/schema";
import { TopicChartsCard } from "@/components/ClusterOverview/TopicChartsCard";

function timeSeriesMetrics(
  ranges: Record<string, { range: string[][]; nodeId?: string }[]> | undefined,
  rangeName: string,
): TimeSeriesMetrics {
  let series: TimeSeriesMetrics = {};

  if (ranges) {
    Object.values(ranges[rangeName] ?? {}).forEach((r) => {
      series = r.range.reduce(
        (a, v) => ({ ...a, [v[0]]: parseFloat(v[1]) }),
        series,
      );
    });
  }

  return series;
}

export async function ConnectedTopicChartsCard({
  cluster,
  topics,
  includeHidden,
}: {
  cluster: Promise<ClusterDetail | null>;
  topics: Promise<ApiResponse<TopicsResponse>>;
  includeHidden?: boolean;
}) {
  const res = await cluster;

  const topicResponse = await topics;

  const topicList =
    topicResponse.payload?.data
      ?.map((topic) => ({
        id: topic.id,
        name: topic.attributes.name,
        managed: topic.meta?.managed,
      }))
      .filter(
        (topic): topic is { id: string; name: string; managed: boolean } =>
          !!topic.id && !!topic.name,
      ) ?? [];

  return (
    <TopicChartsCard
      kafkaId={res?.id}
      topicList={topicList}
      includeHidden={includeHidden}
      isLoading={false}
      isVirtualKafkaCluster={
        res?.meta?.kind === "virtualkafkaclusters.kroxylicious.io"
      }
      incoming={timeSeriesMetrics(
        res?.attributes.metrics?.ranges,
        "incoming_byte_rate",
      )}
      outgoing={timeSeriesMetrics(
        res?.attributes.metrics?.ranges,
        "outgoing_byte_rate",
      )}
    />
  );
}
