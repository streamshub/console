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
import { useFormatBytes } from "@/utils/useFormatBytes";
import { Alert } from "@/libs/patternfly/react-core";
import { useFormatter, useTranslations } from "next-intl";
import { getHeight, getPadding } from "./chartConsts";
import { useChartWidth } from "./useChartWidth";
import { formatDateTime } from "@/utils/dateTime";

type ChartDiskUsageProps = {
  usages: Record<string, TimeSeriesMetrics>;
};

type Datum = {
  x: number;
  y: number;
  name: string;
};

export function ChartMemoryUsage({ usages }: ChartDiskUsageProps) {
  const t = useTranslations();
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 650 ? 6 : width > 300 ? 3 : 1;

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return (
      <Alert
        variant="warning"
        isInline
        isPlain
        title={t("ChartMemoryUsage.data_unavailable")}
      />
    );
  }

  const CursorVoronoiContainer = createContainer("voronoi", "cursor");
  const legendData = Object.keys(usages).map((nodeId) => ({
    name: `Node ${nodeId}`,
    childName: `node ${nodeId}`,
  }));
  const padding = getPadding(legendData.length / itemsPerRow);

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Memory usage"}
        containerComponent={
          <CursorVoronoiContainer
            cursorDimension="x"
            voronoiDimension="x"
            mouseFollowTooltips
            labelComponent={
              <ChartLegendTooltip
                legendData={legendData}
                flyoutWidth={250}
                title={(args) => formatDateTime({ value: args?.x ?? 0 })}
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
          tickFormat={(d) => formatDateTime({ value: d, format: "HH:mm" })}
          tickCount={5}
        />
        <ChartAxis
          dependentAxis
          showGrid={true}
          tickFormat={(d) => {
            return formatBytes(d);
          }}
        />
        <ChartStack>
          {Object.entries(usages).map(([nodeId, series]) => {
            return (
              <ChartArea
                key={ `memory-usage-${nodeId}` }
                data={ Object.entries(series).map(([k, v]) => {
                    return ({
                      name: `Node ${nodeId}`,
                      x: Date.parse(k),
                      y: v,
                    })
                })}
                name={ `node ${nodeId}` }
              />
            );
          })}
        </ChartStack>
      </Chart>
    </div>
  );
}
