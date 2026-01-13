import { ClusterDetail } from "@/api/kafka/schema";
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
}: {
  cluster: Promise<ClusterDetail | null>;
}) {
  const res = await cluster;

  return (
    <TopicChartsCard
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
