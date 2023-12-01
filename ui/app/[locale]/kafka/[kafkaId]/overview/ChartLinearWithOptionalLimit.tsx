"use client";
import type { ChartVoronoiContainerProps } from "@/libs/patternfly/react-charts";
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartGroup,
  ChartLegend,
  ChartThemeColor,
  ChartThreshold,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import {
  chart_color_black_500,
  chart_color_blue_300,
} from "@/libs/patternfly/react-tokens";
import { useFormatter } from "next-intl";
import type { ReactElement } from "react";
import { ChartSkeletonLoader } from "./ChartSkeletonLoader";
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
  metrics: TimeSeriesMetrics;
  chartName: string;
  xLabel?: string;
  yLabel?: string;
  usageLimit?: TimeSeriesMetrics;
  formatValue?: (d: number) => string;
  isLoading: boolean;
  emptyState: ReactElement;
};

export function ChartLinearWithOptionalLimit({
  metrics,
  chartName,
  xLabel,
  yLabel,
  usageLimit,
  formatValue = (d) => `${d}`,
  isLoading,
  emptyState,
}: ChartLinearWithOptionalLimitProps) {
  const format = useFormatter();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width && width > 650 ? 6 : 3;

  const { chartData, legendData, tickValues } = getChartData(
    metrics,
    chartName,
    "Limit",
    usageLimit,
  );

  const hasMetrics = Object.keys(metrics).length > 0;
  // const showDate = shouldShowDate(duration);
  const showDate = false;

  switch (true) {
    case isLoading:
      return <ChartSkeletonLoader height={300} padding={50} />;
    case !hasMetrics:
      return emptyState;
    default: {
      const labels: ChartVoronoiContainerProps["labels"] = ({ datum }) =>
        `${datum.name}: ${formatValue(datum.y)}`;
      return (
        <div ref={containerRef}>
          <Chart
            ariaTitle={chartName}
            containerComponent={
              <ChartVoronoiContainer labels={labels} constrainToVisibleArea />
            }
            legendPosition="bottom-left"
            legendComponent={
              <ChartLegend
                orientation={"horizontal"}
                data={legendData}
                itemsPerRow={itemsPerRow}
              />
            }
            height={350}
            padding={{ bottom: 70, top: 0, left: 80, right: 50 }}
            themeColor={ChartThemeColor.multiUnordered}
            width={width}
            legendAllowWrap={true}
          >
            <ChartAxis
              label={xLabel ? "\n" + (xLabel || "") : undefined}
              // tickValues={tickValues}
              tickFormat={(d) =>
                // dateToChartValue(d, {
                //   showDate,
                // })
                format.dateTime(d, {
                  dateStyle: "short",
                  timeStyle: "short",
                  timeZone: "UTC",
                })
              }
            />
            <ChartAxis
              label={yLabel ? "\n\n\n\n\n" + yLabel : undefined}
              dependentAxis
              tickFormat={formatValue}
            />
            <ChartGroup>
              {chartData.map((value, index) => (
                <ChartArea key={`chart-area-${index}`} data={value.area} />
              ))}
            </ChartGroup>
            <ChartThreshold
              key={`chart-softlimit`}
              data={chartData[0].softLimit}
              style={{
                data: {
                  stroke: chartData[0].softLimitColor,
                },
              }}
            />
          </Chart>
        </div>
      );
    }
  }
}

function getChartData(
  metrics: TimeSeriesMetrics,
  // duration: number,
  lineLabel: string,
  limitLabel: string,
  usageLimit?: TimeSeriesMetrics,
): {
  legendData: Array<LegendData>;
  chartData: Array<ChartData>;
  tickValues: (number | string)[];
} {
  const legendData = [
    usageLimit
      ? {
          name: limitLabel,
          symbol: { fill: "#000000", type: "threshold" },
        }
      : undefined,
    { name: lineLabel, symbol: { fill: "#006600" } },
  ].filter((d) => !!d) as Array<LegendData>;

  const areaColor = chart_color_blue_300.value;
  const softLimitColor = chart_color_black_500.value;
  const chartData: Array<ChartData> = [];
  const area: Array<BrokerChartData> = [];
  const softLimit: Array<BrokerChartData> = [];

  Object.entries(metrics).map(([timestamp, bytes]) => {
    area.push({ name: lineLabel, x: parseInt(timestamp, 10), y: bytes });
  });
  chartData.push({ areaColor, softLimitColor, area, softLimit });

  const tickValues = Object.keys(metrics);

  if (usageLimit) {
    const limits = Object.values(usageLimit);
    tickValues.forEach((timestamp, idx) =>
      softLimit.push({
        name: limitLabel,
        x: parseInt(timestamp, 10),
        y: limits[idx],
      }),
    );
  }

  return {
    legendData,
    chartData,
    tickValues,
  };
}
