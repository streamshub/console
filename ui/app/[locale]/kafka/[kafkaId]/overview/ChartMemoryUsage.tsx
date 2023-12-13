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
import { useFormatBytes } from "@/utils/format";
import { ChartStack } from "@patternfly/react-charts";
import { useFormatter } from "next-intl";
import { useChartWidth } from "./useChartWidth";

type ChartDiskUsageProps = {
  usages: TimeSeriesMetrics[];
};

export function ChartMemoryUsage({ usages }: ChartDiskUsageProps) {
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = 4;

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return <div>TODO</div>;
  }

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Memory usage"}
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
            return formatBytes(d, { maximumFractionDigits: 0 });
          }}
        />
        <ChartStack>
          {usages.map((usage, idx) => {
            const usageArray = Object.entries(usage);
            return (
              <ChartArea
                key={`memory-usage-${idx}`}
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
