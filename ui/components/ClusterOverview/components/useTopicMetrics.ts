import { useEffect, useState } from "react";
import { gettopicMetrics } from "@/api/topics/actions";
import { getKafkaCluster } from "@/api/kafka/actions";

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

type TopicMetricKey = "incoming_byte_rate" | "outgoing_byte_rate";

export function useTopicMetrics(
  kafkaId: string | undefined,
  selectedTopic: string | undefined,
  duration: number,
) {
  const [data, setData] = useState<Record<
    TopicMetricKey,
    TimeSeriesMetrics
  > | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    async function fetchMetrics() {
      if (!kafkaId) return;
      setIsLoading(true);

      try {
        let ranges: any = null;

        if (selectedTopic) {
          const res = await gettopicMetrics(kafkaId, selectedTopic, duration);
          ranges = res.payload?.data?.attributes?.metrics?.ranges;
        } else {
          const res = await getKafkaCluster(kafkaId, { duration });
          ranges = res.payload?.attributes?.metrics?.ranges;
        }

        if (ranges) {
          setData({
            incoming_byte_rate: timeSeriesMetrics(ranges, "incoming_byte_rate"),
            outgoing_byte_rate: timeSeriesMetrics(ranges, "outgoing_byte_rate"),
          });
        } else {
          setData(null);
        }
      } catch (e) {
        console.error("Failed to fetch topic metrics", e);
        setData(null);
      } finally {
        setIsLoading(false);
      }
    }

    fetchMetrics();
  }, [kafkaId, selectedTopic, duration]);

  return { data, isLoading };
}
