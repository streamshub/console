import { useEffect, useState } from "react";
import { getNodeMetrics } from "@/api/nodes/actions";
import { timeSeriesMetrics } from "@/components/ClusterOverview/components/timeSeriesMetrics";

type MetricKey =
  | "volume_stats_used_bytes"
  | "volume_stats_capacity_bytes"
  | "cpu_usage_seconds"
  | "memory_usage_bytes";

export function useNodeMetrics(
  kafkaId: string | undefined,
  selectedBroker?: string,
) {
  const [data, setData] = useState<Record<
    MetricKey,
    Record<string, TimeSeriesMetrics>
  > | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    async function fetchMetrics() {
      if (!kafkaId || !selectedBroker) {
        setData(null);
        return;
      }

      const nodeId = selectedBroker.replace("Node ", "");
      setIsLoading(true);

      try {
        const res = await getNodeMetrics(kafkaId, nodeId);
        const ranges = res.payload?.data?.attributes?.metrics?.ranges;

        if (!ranges) {
          setData(null);
          return;
        }

        setData({
          volume_stats_used_bytes: timeSeriesMetrics(
            ranges,
            "volume_stats_used_bytes",
          ),
          volume_stats_capacity_bytes: timeSeriesMetrics(
            ranges,
            "volume_stats_capacity_bytes",
          ),
          cpu_usage_seconds: timeSeriesMetrics(ranges, "cpu_usage_seconds"),
          memory_usage_bytes: timeSeriesMetrics(ranges, "memory_usage_bytes"),
        });
      } catch (e) {
        console.error("Failed to fetch node metrics", e);
        setData(null);
      } finally {
        setIsLoading(false);
      }
    }

    fetchMetrics();
  }, [kafkaId, selectedBroker]);

  return { data, isLoading };
}
