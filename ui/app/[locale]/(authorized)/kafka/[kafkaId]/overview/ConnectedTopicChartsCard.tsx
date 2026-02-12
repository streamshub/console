import { ApiResponse } from "@/api/api";
import { ClusterDetail } from "@/api/kafka/schema";
import { TopicsResponse } from "@/api/topics/schema";
import {
  TopicChartsCard,
  TopicOption,
} from "@/components/ClusterOverview/TopicChartsCard";
import { Visibility } from "@patternfly/react-table";

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

  const topicList: TopicOption[] =
    topicResponse.payload?.data
      ?.filter(
        (
          topic,
        ): topic is typeof topic & {
          id: string;
          attributes: { name: string };
        } => !!topic.id && !!topic.attributes?.name,
      )
      .map((topic) => ({
        id: topic.id,
        name: topic.attributes.name,
        visibility: topic.attributes.visibility,
        managed: topic.meta?.managed,
      })) ?? [];

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
