"use client";
import { ChartCpuUsage } from "@/components/ClusterOverview/components/ChartCpuUsage";
import { ChartDiskUsage } from "@/components/ClusterOverview/components/ChartDiskUsage";
import { ChartMemoryUsage } from "@/components/ClusterOverview/components/ChartMemoryUsage";
import { ChartSkeletonLoader } from "@/components/ClusterOverview/components/ChartSkeletonLoader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Divider,
  Flex,
  Title,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

type ClusterChartsCardProps = {
  usedDiskSpace: TimeSeriesMetrics[];
  availableDiskSpace: TimeSeriesMetrics[];
  memoryUsage: TimeSeriesMetrics[];
  cpuUsage: TimeSeriesMetrics[];
};

export function ClusterChartsCard({
  isLoading,
  usedDiskSpace,
  availableDiskSpace,
  memoryUsage,
  cpuUsage,
}:
  | ({ isLoading: false } & ClusterChartsCardProps)
  | ({
      isLoading: true;
    } & Partial<{ [key in keyof ClusterChartsCardProps]?: undefined }>)) {
  const t = useTranslations();
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
        <Flex direction={{ default: "column" }} gap={{ default: "gapLg" }}>
          <b>
            {t("ClusterChartsCard.used_disk_space")}{" "}
            <Tooltip content={t("ClusterChartsCard.used_disk_space_tooltip")}>
              <HelpIcon />
            </Tooltip>
          </b>
          {isLoading ? (
            <ChartSkeletonLoader />
          ) : (
            <ChartDiskUsage
              usages={usedDiskSpace}
              available={availableDiskSpace}
            />
          )}
          <Divider />
          <b>
            {t("ClusterChartsCard.cpu_usage")}{" "}
            <Tooltip content={t("ClusterChartsCard.cpu_usage_tooltip")}>
              <HelpIcon />
            </Tooltip>
          </b>
          {isLoading ? (
            <ChartSkeletonLoader />
          ) : (
            <ChartCpuUsage usages={cpuUsage} />
          )}
          <Divider />
          <b>
            {t("ClusterChartsCard.memory_usage")}{" "}
            <Tooltip content={t("ClusterChartsCard.memory_usage_tooltip")}>
              <HelpIcon />
            </Tooltip>
          </b>
          {isLoading ? (
            <ChartSkeletonLoader />
          ) : (
            <ChartMemoryUsage usages={memoryUsage} />
          )}
        </Flex>
      </CardBody>
    </Card>
  );
}
