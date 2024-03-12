"use client";
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartLegend,
  ChartLegendTooltip,
  ChartStack,
  ChartThemeColor,
  createContainer,
} from "@/libs/patternfly/react-charts";
import { useFormatter, useTranslations } from "next-intl";
import { getHeight, getPadding } from "./chartConsts";
import { useChartWidth } from "./useChartWidth";

type ChartCpuUsageProps = {
  usages: TimeSeriesMetrics[];
};

type Datum = {
  x: number;
  y: number;
  name: string;
};

export function ChartCpuUsage({ usages }: ChartCpuUsageProps) {
  const t = useTranslations();
  const format = useFormatter();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 650 ? 6 : width > 300 ? 3 : 1;

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return <div>{t("ChartCpuUsage.data_unavailable")}</div>;
  }
  // const showDate = shouldShowDate(duration);
  const CursorVoronoiContainer = createContainer("voronoi", "cursor");
  const legendData = usages.map((_, idx) => ({
    name: `Node ${idx}`,
    childName: `node ${idx}`,
  }));
  const padding = getPadding(legendData.length / itemsPerRow);
  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Cpu usage"}
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
              datum.y !== null ? `${format.number(datum.y * 1000)}m` : "no data"
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
            return format.number(d * 1000) + "m";
          }}
        />
        <ChartStack>
          {usages.map((usage, idx) => {
            const usageArray = Object.entries(usage);
            return (
              <ChartArea
                key={`cpu-usage-${idx}}`}
                data={usageArray.map(([x, y]) => ({
                  name: `Node ${idx + 1}`,
                  x,
                  y,
                }))}
                name={`node ${idx}`}
              />
            );
          })}
        </ChartStack>
      </Chart>
    </div>
  );
}
