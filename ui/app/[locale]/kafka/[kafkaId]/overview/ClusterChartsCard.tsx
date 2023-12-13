"use client";
import {
  ChartDiskUsage,
  TimeSeriesMetrics,
} from "@/app/[locale]/kafka/[kafkaId]/overview/ChartDiskUsage";
import { ChartSkeletonLoader } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartSkeletonLoader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/libs/patternfly/react-core";
import { useFormatBytes } from "@/utils/format";

type ClusterChartsCardProps = {
  usedDiskSpace: TimeSeriesMetrics;
  availableDiskSpace: TimeSeriesMetrics;
};

export function ClusterChartsCard({
  isLoading,
  usedDiskSpace,
  availableDiskSpace,
}:
  | ({ isLoading: false } & ClusterChartsCardProps)
  | ({
      isLoading: true;
    } & Partial<{ [key in keyof ClusterChartsCardProps]?: undefined }>)) {
  const formatter = useFormatBytes();
  return (
    <Card>
      <CardHeader>
        <CardTitle>Available disk space</CardTitle>
      </CardHeader>
      <CardBody>
        {isLoading && <ChartSkeletonLoader height={300} padding={50} />}
        {!isLoading && (
          <ChartDiskUsage
            usage={usedDiskSpace}
            available={availableDiskSpace}
          />
        )}
      </CardBody>
    </Card>
  );
}
