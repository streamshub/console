"use client";

import {
  Alert,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Title,
} from "@/libs/patternfly/react-core";

import { useTranslations } from "next-intl";
import { ClusterDetail } from "@/api/kafka/schema";
import { ClusterChartsCard } from "@/components/ClusterOverview/ClusterChartsCard";

function timeSeriesMetrics(
  ranges: Record<string, { range: string[][]; nodeId?: string }[]> | undefined,
  rangeName: string,
): Record<string, TimeSeriesMetrics> {
  const series: Record<string, TimeSeriesMetrics> = {};

  if (ranges) {
    Object.values(ranges[rangeName] ?? {}).forEach((r) => {
      series[r.nodeId!] = r.range.reduce(
        (a, v) => ({ ...a, [v[0]]: parseFloat(v[1]) }),
        {} as TimeSeriesMetrics,
      );
    });
  }

  return series;
}

export async function ConnectedClusterChartsCard({
  cluster,
}: {
  cluster: Promise<ClusterDetail | null>;
}) {
  const t = useTranslations();
  const res = await cluster;

  const isVirtualKafkaCluster =
    res?.meta?.kind === "virtualkafkaclusters.kroxylicious.io";

  const metricsUnavailable = res?.attributes.metrics === null;

  console.log("is virtual kafka cluster", isVirtualKafkaCluster);

  if (metricsUnavailable || isVirtualKafkaCluster) {
    /*
     * metrics being null (rather than undefined or empty) is how the server
     * indicates that metrics are not configured for this cluster.
     */

    const alertTitle = isVirtualKafkaCluster
      ? t("ClusterChartsCard.virtual_cluster_metrics_unavailable")
      : t("ClusterChartsCard.data_unavailable");

    return (
      <Card>
        <CardHeader>
          <CardTitle>
            <Title headingLevel={"h2"} size={"lg"}>
              {t("ClusterChartsCard.cluster_metrics")}
            </Title>
          </CardTitle>
        </CardHeader>
        <CardBody>
          <Alert
            variant={isVirtualKafkaCluster ? "info" : "warning"}
            isInline
            isPlain
            title={alertTitle}
          />
        </CardBody>
      </Card>
    );
  }

  return (
    <ClusterChartsCard
      isLoading={false}
      usedDiskSpace={timeSeriesMetrics(
        res?.attributes.metrics?.ranges,
        "volume_stats_used_bytes",
      )}
      availableDiskSpace={timeSeriesMetrics(
        res?.attributes.metrics?.ranges,
        "volume_stats_capacity_bytes",
      )}
      memoryUsage={timeSeriesMetrics(
        res?.attributes.metrics?.ranges,
        "memory_usage_bytes",
      )}
      cpuUsage={timeSeriesMetrics(
        res?.attributes.metrics?.ranges,
        "cpu_usage_seconds",
      )}
    />
  );
}
