"use client";
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartLegend,
  ChartThemeColor,
  ChartThreshold,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/format";
import { chart_color_orange_300 } from "@patternfly/react-tokens";
import { useFormatter } from "next-intl";
import { useChartWidth } from "./useChartWidth";

export type TimeSeriesMetrics = { [timestamp: string]: number };

type ChartData = {
  areaColor: string;
  softLimitColor: string;
  area: BrokerChartData[];
  softLimit: BrokerChartData[];
};

type BrokerChartData = {
  name: string;
  x: number | string;
  y: number;
};

type LegendData = {
  name: string;
  symbol?: {
    fill?: string;
    type?: string;
  };
};

type ChartLinearWithOptionalLimitProps = {
  usage: TimeSeriesMetrics;
  available: TimeSeriesMetrics;
};

export function ChartDiskUsage({
  usage,
  available,
}: ChartLinearWithOptionalLimitProps) {
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width && width > 650 ? 6 : 3;

  const hasMetrics = Object.keys(usage).length > 0;
  if (!hasMetrics) {
    return <div>TODO</div>;
  }
  // const showDate = shouldShowDate(duration);
  const usageArray = Object.entries(usage);
  const maxUsage = Math.max(...Object.values(usage));

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Available disk space"}
        containerComponent={
          <ChartVoronoiContainer
            labels={({ datum }) => `${datum.name}: ${formatBytes(datum.y)}`}
            constrainToVisibleArea
          />
        }
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend
            orientation={"horizontal"}
            data={[
              { name: "Used storage" },
              {
                name: "Available storage threshold",
                symbol: { fill: chart_color_orange_300.var, type: "threshold" },
              },
            ]}
            itemsPerRow={itemsPerRow}
          />
        }
        height={350}
        padding={{ bottom: 70, top: 0, left: 90, right: 20 }}
        themeColor={ChartThemeColor.multiUnordered}
        width={width}
        legendAllowWrap={true}
      >
        <ChartAxis
          scale={"time"}
          tickFormat={(d) => {
            const [_, time] = format
              .dateTime(d, {
                dateStyle: "short",
                timeStyle: "short",
                timeZone: "UTC",
              })
              .split(" ");
            return time;
          }}
        />
        <ChartAxis
          dependentAxis
          showGrid={true}
          tickFormat={(d) => {
            return formatBytes(d);
          }}
        />
        <ChartArea
          data={usageArray.map(([x, y]) => ({
            name: "Used storage",
            x,
            y,
          }))}
        />
        <ChartThreshold
          key={`chart-softlimit`}
          data={Object.entries(available).map(([_, y], idx) => ({
            name: "Available storage threshold",
            x: usageArray[idx][0],
            y,
          }))}
          style={{
            data: {
              stroke: chart_color_orange_300.var,
            },
          }}
        />
      </Chart>
    </div>
  );
}
