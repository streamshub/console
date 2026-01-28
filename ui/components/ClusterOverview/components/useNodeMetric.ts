import { useEffect, useState } from "react";
import { getNodeMetrics } from "@/api/nodes/actions";
import { timeSeriesMetrics } from "@/components/ClusterOverview/components/timeSeriesMetrics";
import { getKafkaCluster } from "@/api/kafka/actions";

type MetricKey =
  | "volume_stats_used_bytes"
  | "volume_stats_capacity_bytes"
  | "cpu_usage_seconds"
  | "memory_usage_bytes";

export function useNodeMetrics(
  kafkaId: string | undefined,
  selectedBroker: string | undefined,
  duration: number,
) {
  const [data, setData] = useState<Record<
    MetricKey,
    Record<string, TimeSeriesMetrics>
  > | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    async function fetchMetrics() {
      if (!kafkaId) return;
      setIsLoading(true);

      try {
        let ranges: any = null;

        if (selectedBroker) {
          const nodeId = selectedBroker.replace("Node ", "");
          const res = await getNodeMetrics(kafkaId, nodeId, duration);
          ranges = res.payload?.data?.attributes?.metrics?.ranges;
        } else {
          const res = await getKafkaCluster(kafkaId, { duration });
          ranges = res.payload?.attributes?.metrics?.ranges;
        }

        if (ranges) {
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
        } else {
          setData(null);
        }
      } catch (e) {
        console.error("Failed to fetch metrics", e);
        setData(null);
      } finally {
        setIsLoading(false);
      }
    }

    fetchMetrics();
  }, [kafkaId, selectedBroker, duration]);

  return { data, isLoading };
}
