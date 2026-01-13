"use client";
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartGroup,
  ChartLegend,
  ChartLegendTooltip,
  ChartThemeColor,
  createContainer,
} from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/useFormatBytes";
import { Alert } from "@/libs/patternfly/react-core";
import { useFormatter, useTranslations } from "next-intl";
import { getHeight, getPadding } from "./chartConsts";
import { useChartWidth } from "./useChartWidth";
import { formatDateTime } from "@/utils/dateTime";

type ChartIncomingOutgoingProps = {
  incoming: TimeSeriesMetrics;
  outgoing: TimeSeriesMetrics;
  isVirtualKafkaCluster: boolean;
};

type Datum = {
  x: number;
  y: number;
  value: number;
  name: string;
};

export function ChartIncomingOutgoing({
  incoming,
  outgoing,
  isVirtualKafkaCluster,
}: ChartIncomingOutgoingProps) {
  const t = useTranslations();
  const formatBytes = useFormatBytes();
  const format = useFormatter();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 500 ? 2 : 1;

  const hasMetrics =
    Object.keys(incoming).length > 0 && Object.keys(outgoing).length > 0;
  if (!hasMetrics || isVirtualKafkaCluster) {
    return (
      <Alert
        variant={isVirtualKafkaCluster ? "info" : "warning"}
        isInline
        isPlain
        title={
          isVirtualKafkaCluster
            ? t("ClusterChartsCard.virtual_cluster_metrics_unavailable")
            : t("ChartIncomingOutgoing.data_unavailable")
        }
      />
    );
  }
  // const showDate = shouldShowDate(duration);
  const CursorVoronoiContainer = createContainer("voronoi", "cursor");
  const legendData = [
    {
      name: "Incoming bytes (all topics)",
      childName: "incoming",
    },
    {
      name: "Outgoing bytes (all topics)",
      childName: "outgoing",
    },
  ];

  const padding = getPadding(legendData.length / itemsPerRow);
  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={"Topics bytes incoming and outgoing"}
        containerComponent={
          <CursorVoronoiContainer
            cursorDimension="x"
            voronoiDimension="x"
            mouseFollowTooltips
            labelComponent={
              <ChartLegendTooltip
                legendData={legendData}
                title={(args) => formatDateTime({ value: args?.x ?? 0 })}
              />
            }
            labels={({ datum }: { datum: Datum }) => {
              return datum.value !== null
                ? formatBytes(datum.value)
                : "no data";
            }}
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
          tickCount={4}
          orientation={"bottom"}
          offsetY={padding.bottom}
        />
        <ChartAxis
          dependentAxis
          tickFormat={(d) => {
            return formatBytes(Math.abs(d));
          }}
        />
        <ChartGroup>
          <ChartArea
            key={`incoming-line`}
            data={Object.entries(incoming).map(([k, v]) => {
              return {
                name: `Incoming`,
                x: Date.parse(k),
                y: v,
                value: v,
              };
            })}
            name={`incoming`}
            interpolation={"stepAfter"}
          />
          <ChartArea
            key={`outgoing-line`}
            data={Object.entries(outgoing).map(([k, v]) => {
              return {
                name: `Outgoing`,
                x: Date.parse(k),
                y: v * -1,
                value: v,
              };
            })}
            name={`outgoing`}
            interpolation={"stepAfter"}
          />
        </ChartGroup>
      </Chart>
    </div>
  );
}
