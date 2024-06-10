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
import { useFormatter, useTranslations } from "next-intl";
import { getHeight, getPadding } from "./chartConsts";
import { useChartWidth } from "./useChartWidth";

type ChartDiskUsageProps = {
  usages: TimeSeriesMetrics[];
  available: TimeSeriesMetrics[];
};
type Datum = {
  x: number;
  y: number;
  name: string;
};

export function ChartDiskUsage({ usages, available }: ChartDiskUsageProps) {
  const t = useTranslations();
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 650 ? 2 : 1;

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return <div>{t("ChartDiskUsage.data_unavailable")}</div>;
  }
  const CursorVoronoiContainer = createContainer("voronoi", "cursor");
  const legendData = [
    ...usages.map((_, idx) => ({
      name: `Node ${idx}`,
      childName: `node ${idx}`,
    })),
    ...usages.map((_, idx) => ({
      name: `Available storage threshold (node ${idx})`,
      childName: `threshold ${idx}`,
      symbol: { type: "threshold" },
    })),
  ];
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
                  format.dateTime(args?.x ?? 0, {
                    timeZone: "UTC",
                    timeStyle: "medium",
                    dateStyle: "short",
                  })
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
          tickFormat={(d) => {
            const [_, ...time] = format
              .dateTime(d, {
                dateStyle: "short",
                timeStyle: "short",
                timeZone: "UTC",
              })
              .split(" ");
            return time.join(" ");
          }}
          tickCount={5}
        />
        <ChartAxis
          dependentAxis
          showGrid={true}
          tickFormat={(d) => {
            return formatBytes(d, { maximumFractionDigits: 0 });
          }}
        />
        <ChartGroup>
          {usages.map((usage, idx) => {
            const usageArray = Object.entries(usage);
            return (
              <ChartArea
                key={`usage-area-${idx}`}
                data={usageArray.map(([x, y]) => ({
                  name: `Node ${idx + 1}`,
                  x,
                  y,
                }))}
                name={`node ${idx}`}
              />
            );
          })}
          {usages.map((usage, idx) => {
            const usageArray = Object.entries(usage);
            const data = Object.entries(available[idx]);
            return (
              <ChartThreshold
                key={`chart-softlimit-${idx}}`}
                data={data.map(([_, y], x) => ({
                  name: `Available storage threshold (node ${idx + 1})`,
                  x: usageArray[x][0],
                  y,
                }))}
                name={`threshold ${idx}`}
              />
            );
          })}
        </ChartGroup>
      </Chart>
    </div>
  );
}
