import {
  Alert,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Title,
} from "@/libs/patternfly/react-core";
import { getTranslations } from "next-intl/server";
import { ClusterDetail } from "@/api/kafka/schema";
import { ClusterChartsCard } from "@/components/ClusterOverview/ClusterChartsCard";
import { timeSeriesMetrics } from "@/components/ClusterOverview/components/timeSeriesMetrics";

export async function ConnectedClusterChartsCard({
  cluster,
}: {
  cluster: Promise<ClusterDetail | null>;
}) {
  const t = await getTranslations();
  const res = await cluster;

  const isVirtualKafkaCluster =
    res?.meta?.kind === "virtualkafkaclusters.kroxylicious.io";

  const brokerList =
    res?.relationships?.nodes?.data?.map((n) => `Node ${n.id}`) ?? [];

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
      kafkaId={res?.id}
      isLoading={false}
      brokerList={brokerList}
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
