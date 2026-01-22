"use client";
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartGroup,
  ChartLegend,
  ChartLegendTooltip,
  ChartThemeColor,
  ChartThreshold,
  createContainer,
} from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/useFormatBytes";
import { Alert } from "@/libs/patternfly/react-core";
import { useFormatter, useTranslations } from "next-intl";
import { getHeight, getPadding } from "./chartConsts";
import { useChartWidth } from "./useChartWidth";
import { formatDateTime } from "@/utils/dateTime";
import { DurationOptions } from "./FilterByTime";

type ChartDiskUsageProps = {
  usages: Record<string, TimeSeriesMetrics>;
  available: Record<string, TimeSeriesMetrics>;
  duration: DurationOptions;
};
type Datum = {
  x: number;
  y: number;
  name: string;
};

export function ChartDiskUsage({
  usages,
  available,
  duration,
}: ChartDiskUsageProps) {
  const t = useTranslations();
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 650 ? 2 : 1;

  const showDate = duration >= DurationOptions.Last24hours;
  const axisFormat = showDate ? "HH:mm'\n'MMM dd" : "HH:mm";
  const tooltipFormat = showDate ? "MMM dd, HH:mm" : "HH:mm";

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return (
      <Alert
        variant="warning"
        isInline
        isPlain
        title={t("ChartDiskUsage.data_unavailable")}
      />
    );
  }
  const CursorVoronoiContainer = createContainer("voronoi", "cursor");
  const legendData: {
    name: string;
    childName: string;
    symbol?: { type: string };
  }[] = [];

  Object.entries(usages).forEach(([nodeId, _]) => {
    legendData.push({
      name: `Node ${nodeId}`,
      childName: `node ${nodeId}`,
    });
  });

  Object.entries(usages).forEach(([nodeId, _]) => {
    legendData.push({
      name: `Available storage threshold (node ${nodeId})`,
      childName: `threshold ${nodeId}`,
      symbol: { type: "threshold" },
    });
  });

  const padding = getPadding(legendData.length / itemsPerRow);
  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Used disk space"}
        containerComponent={
          <CursorVoronoiContainer
            cursorDimension="x"
            voronoiDimension="x"
            mouseFollowTooltips
            labelComponent={
              <ChartLegendTooltip
                legendData={legendData}
                title={(args) =>
                  formatDateTime({ value: args?.x ?? 0, format: tooltipFormat })
                }
              />
            }
            labels={({ datum }: { datum: Datum }) =>
              datum.y !== null ? formatBytes(datum.y) : "no data"
            }
            constrainToVisibleArea
          />
        }
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend
            orientation={"horizontal"}
            data={legendData}
            itemsPerRow={itemsPerRow}
          />
        }
        height={getHeight(legendData.length / itemsPerRow)}
        padding={padding}
        themeColor={ChartThemeColor.multiUnordered}
        width={width}
        legendAllowWrap={true}
      >
        <ChartAxis
          scale={"time"}
          tickFormat={(d) => formatDateTime({ value: d, format: axisFormat })}
          tickCount={5}
        />
        <ChartAxis
          dependentAxis
          showGrid={true}
          tickFormat={(d) => {
            return formatBytes(d);
          }}
        />
        <ChartGroup>
          {Object.entries(usages).map(([nodeId, series]) => {
            return (
              <ChartArea
                key={`usage-area-${nodeId}`}
                data={Object.entries(series).map(([k, v]) => {
                  return {
                    name: `Node ${nodeId}`,
                    x: Date.parse(k),
                    y: v,
                  };
                })}
                name={`node ${nodeId}`}
              />
            );
          })}

          {Object.entries(usages).map(([nodeId, _]) => {
            const availableSeries = available[nodeId];

            return (
              <ChartThreshold
                key={`chart-softlimit-${nodeId}`}
                data={Object.entries(availableSeries).map(([k, v]) => ({
                  name: `Available storage threshold (node ${nodeId})`,
                  x: Date.parse(k),
                  y: v,
                }))}
                name={`threshold ${nodeId}`}
              />
            );
          })}
        </ChartGroup>
      </Chart>
    </div>
  );
}
