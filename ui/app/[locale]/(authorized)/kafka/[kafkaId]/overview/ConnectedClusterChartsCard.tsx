import { ClusterMetric } from "@/api/kafka/actions";
import { ClusterDetail, MetricRange } from "@/api/kafka/schema";
import { ClusterChartsCard } from "@/components/ClusterOverview/ClusterChartsCard";

function timeSeriesMetrics(
  ranges: Record<ClusterMetric, MetricRange> | undefined,
  rangeName: ClusterMetric,
): TimeSeriesMetrics[] {
  return ranges
    ? Object.values(ranges[rangeName] ?? {}).map((val) => val ?? {})
    : [];
}

export async function ConnectedClusterChartsCard({
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
