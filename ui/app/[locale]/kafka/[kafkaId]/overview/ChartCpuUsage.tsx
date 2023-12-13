"use client";
import {
  height,
  padding,
} from "@/app/[locale]/kafka/[kafkaId]/overview/chartConsts";
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartLegend,
  ChartThemeColor,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import { ChartStack } from "@patternfly/react-charts";
import { useFormatter } from "next-intl";
import { useChartWidth } from "./useChartWidth";

type ChartCpuUsageProps = {
  usages: TimeSeriesMetrics[];
};

export function ChartCpuUsage({ usages }: ChartCpuUsageProps) {
  const format = useFormatter();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = 4;

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return <div>TODO</div>;
  }
  // const showDate = shouldShowDate(duration);

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Cpu usage"}
        containerComponent={
          <ChartVoronoiContainer
            labels={({ datum }) =>
              `${datum.name}: ${format.number(datum.y * 1000)}m`
            }
            constrainToVisibleArea
          />
        }
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend
            orientation={"horizontal"}
            data={usages.map((_, idx) => ({ name: `Node ${idx}` }))}
            itemsPerRow={itemsPerRow}
          />
        }
        height={height}
        padding={padding}
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
              />
            );
          })}
        </ChartStack>
      </Chart>
    </div>
  );
}
