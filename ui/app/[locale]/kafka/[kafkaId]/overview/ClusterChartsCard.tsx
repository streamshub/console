"use client";
import {
  ChartLinearWithOptionalLimit,
  TimeSeriesMetrics,
} from "@/app/[locale]/kafka/[kafkaId]/overview/ChartLinearWithOptionalLimit";
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
        {isLoading && (
          <ChartLinearWithOptionalLimit
            metrics={{}}
            chartName={"Available disk space"}
            isLoading={true}
            emptyState={<div>empty</div>}
          />
        )}
        {!isLoading && (
          <ChartLinearWithOptionalLimit
            metrics={usedDiskSpace}
            usageLimit={availableDiskSpace}
            chartName={"Available disk space"}
            isLoading={false}
            emptyState={<div>empty</div>}
            formatValue={(v) => formatter(v)}
          />
        )}
      </CardBody>
    </Card>
  );
}
