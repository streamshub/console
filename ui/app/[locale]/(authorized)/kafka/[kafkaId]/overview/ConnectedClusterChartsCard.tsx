import { ClusterDetail } from "@/api/kafka/schema";
import { ClusterChartsCard } from "@/components/ClusterOverview/ClusterChartsCard";

function timeSeriesMetrics(
  ranges: Record<string, { range: string[][]; nodeId?: string; }[]> | undefined,
  rangeName: string,
): Record<string, TimeSeriesMetrics> {
  const series: Record<string, TimeSeriesMetrics> = {};

  if (ranges) {
    Object.values(ranges[rangeName] ?? {}).forEach((r) => {
      series[r.nodeId!] = r.range.reduce((a, v) => ({ ...a, [v[0]]: parseFloat(v[1]) }), {} as TimeSeriesMetrics);
    });
  }

  return series;
}

export async function ConnectedClusterChartsCard({
  cluster,
}: {
  cluster: Promise<ClusterDetail | null>;
}) {
  const res = await cluster;

  return (
    <ClusterChartsCard
      isLoading={ false }
      usedDiskSpace={ timeSeriesMetrics(res?.attributes.metrics?.ranges, "volume_stats_used_bytes") }
      availableDiskSpace={ timeSeriesMetrics(res?.attributes.metrics?.ranges, "volume_stats_capacity_bytes") }
      memoryUsage={ timeSeriesMetrics(res?.attributes.metrics?.ranges, "memory_usage_bytes") }
      cpuUsage={ timeSeriesMetrics(res?.attributes.metrics?.ranges, "cpu_usage_seconds") }
    />
  );
}
