"use client";
import { ChartCpuUsage } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartCpuUsage";
import { ChartDiskUsage } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartDiskUsage";
import { ChartMemoryUsage } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartMemoryUsage";
import { ChartSkeletonLoader } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartSkeletonLoader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Divider,
  Flex,
} from "@/libs/patternfly/react-core";
import { Title, Tooltip } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";

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
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Title headingLevel={"h2"} size={"lg"}>
            Cluster metrics
          </Title>
        </CardTitle>
      </CardHeader>
      <CardBody>
        <Flex direction={{ default: "column" }} gap={{ default: "gapLg" }}>
          <b>
            Used disk space{" "}
            <Tooltip
              content={
                "Used and available disk capacity for all brokers over a specified period. Make sure there's enough space for everyday operations."
              }
            >
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
            CPU usage{" "}
            <Tooltip
              content={
                "CPU utilization for all brokers over a specified period. Sustained high usage may indicate the need for resource optimization. Sustained high usage may indicate the need to review cluster capacity, producer send, or consumer fetch configurations."
              }
            >
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
            Memory usage{" "}
            <Tooltip
              content={
                "Memory utilization for all brokers over a specified period. Efficient memory allocation is essential for optimal performance of Kafka."
              }
            >
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
