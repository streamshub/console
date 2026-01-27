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
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { FilterByBroker } from "./components/FilterByBroker";
import { useState } from "react";
import { useNodeMetrics } from "./components/useNodeMetric";
import {FilterByTime } from "./components/FilterByTime";
import { DurationOptions } from "./components/type";

function hasMetrics(metrics?: Record<string, TimeSeriesMetrics>) {
  return metrics && Object.keys(metrics).length > 0;
}

type ClusterChartsCardProps = {
  brokerList: string[];
  usedDiskSpace: Record<string, TimeSeriesMetrics>;
  availableDiskSpace: Record<string, TimeSeriesMetrics>;
  memoryUsage: Record<string, TimeSeriesMetrics>;
  cpuUsage: Record<string, TimeSeriesMetrics>;
  kafkaId: string | undefined;
};

export function ClusterChartsCard({
  isLoading,
  usedDiskSpace,
  availableDiskSpace,
  memoryUsage,
  cpuUsage,
  brokerList,
  kafkaId,
}:
  | ({ isLoading: false } & ClusterChartsCardProps)
  | ({
      isLoading: true;
    } & Partial<{ [key in keyof ClusterChartsCardProps]?: undefined }>)) {
  const t = useTranslations();

  const [diskBroker, setDiskBroker] = useState<string>();
  const [cpuBroker, setCpuBroker] = useState<string>();
  const [memBroker, setMemBroker] = useState<string>();

  const [diskDuration, setDiskDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes,
  );
  const [cpuDuration, setCpuDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes,
  );
  const [memDuration, setMemDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes,
  );

  const diskMetrics = useNodeMetrics(kafkaId, diskBroker, diskDuration);
  const cpuMetrics = useNodeMetrics(kafkaId, cpuBroker, cpuDuration);
  const memMetrics = useNodeMetrics(kafkaId, memBroker, memDuration);

  const diskHasMetrics = hasMetrics(
    diskMetrics.data?.volume_stats_used_bytes ?? usedDiskSpace,
  );

  const cpuHasMetrics = hasMetrics(
    cpuMetrics.data?.cpu_usage_seconds ?? cpuUsage,
  );

  const memHasMetrics = hasMetrics(
    memMetrics.data?.memory_usage_bytes ?? memoryUsage,
  );

  const disableFilter = isLoading || !kafkaId || brokerList.length <= 1;

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
            <>
              {!isLoading && diskHasMetrics && (
                <Toolbar>
                  <ToolbarContent>
                    <ToolbarGroup variant="filter-group">
                      <FilterByBroker
                        brokerList={brokerList}
                        selectedBroker={diskBroker}
                        onSetSelectedBroker={setDiskBroker}
                        disableToolbar={disableFilter || diskMetrics.isLoading}
                      />
                      <FilterByTime
                        duration={diskDuration}
                        onDurationChange={setDiskDuration}
                        ariaLabel={"Select time range"}
                        disableToolbar={disableFilter || diskMetrics.isLoading}
                      />
                    </ToolbarGroup>
                  </ToolbarContent>
                </Toolbar>
              )}
              <ChartDiskUsage
                duration={diskDuration}
                usages={
                  diskMetrics.data?.volume_stats_used_bytes ?? usedDiskSpace
                }
                available={
                  diskMetrics.data?.volume_stats_capacity_bytes ??
                  availableDiskSpace
                }
              />
            </>
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
            <>
              {!isLoading && cpuHasMetrics && (
                <Toolbar>
                  <ToolbarContent>
                    <ToolbarGroup variant="filter-group">
                      <FilterByBroker
                        brokerList={brokerList}
                        selectedBroker={cpuBroker}
                        onSetSelectedBroker={setCpuBroker}
                        disableToolbar={disableFilter || cpuMetrics.isLoading}
                      />
                      <FilterByTime
                        duration={cpuDuration}
                        onDurationChange={setCpuDuration}
                        ariaLabel={"Select cpu time range"}
                        disableToolbar={disableFilter || cpuMetrics.isLoading}
                      />
                    </ToolbarGroup>
                  </ToolbarContent>
                </Toolbar>
              )}

              <ChartCpuUsage
                duration={cpuDuration}
                usages={cpuMetrics.data?.cpu_usage_seconds ?? cpuUsage}
              />
            </>
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
            <>
              {!isLoading && memHasMetrics && (
                <Toolbar>
                  <ToolbarContent>
                    <ToolbarGroup variant="filter-group">
                      <FilterByBroker
                        brokerList={brokerList}
                        selectedBroker={memBroker}
                        onSetSelectedBroker={setMemBroker}
                        disableToolbar={disableFilter || memMetrics.isLoading}
                      />
                      <FilterByTime
                        duration={memDuration}
                        onDurationChange={setMemDuration}
                        ariaLabel={"Select memory time range"}
                        disableToolbar={disableFilter || memMetrics.isLoading}
                      />
                    </ToolbarGroup>
                  </ToolbarContent>
                </Toolbar>
              )}
              <ChartMemoryUsage
                duration={memDuration}
                usages={memMetrics.data?.memory_usage_bytes ?? memoryUsage}
              />
            </>
          )}
        </Flex>
      </CardBody>
    </Card>
  );
}
