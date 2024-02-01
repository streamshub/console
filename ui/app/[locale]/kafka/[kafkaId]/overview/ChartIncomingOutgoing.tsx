"use client";
import {
  getHeight,
  getPadding,
} from "@/app/[locale]/kafka/[kafkaId]/overview/chartConsts";
import {
  Chart,
  ChartAxis,
  ChartGroup,
  ChartLegend,
  ChartLegendTooltip,
  ChartThemeColor,
  createContainer,
} from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/format";
import { ChartArea } from "@patternfly/react-charts";
import { useFormatter } from "next-intl";
import { useChartWidth } from "./useChartWidth";

type ChartIncomingOutgoingProps = {
  incoming: Record<string, TimeSeriesMetrics | undefined>;
  outgoing: Record<string, TimeSeriesMetrics | undefined>;
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
}: ChartIncomingOutgoingProps) {
  const formatBytes = useFormatBytes();
  const format = useFormatter();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 500 ? 2 : 1;

  const hasMetrics =
    Object.keys(incoming).length > 0 && Object.keys(outgoing).length > 0;
  if (!hasMetrics) {
    return <div><i>Not available</i></div>;
  }
  // const showDate = shouldShowDate(duration);
  const CursorVoronoiContainer = createContainer("voronoi", "cursor");
  const legendData = [
    ...Object.keys(incoming).map((name) => ({
      name: `Incoming bytes (${name})`,
      childName: `incoming ${name}`,
    })),
    ...Object.keys(outgoing).map((name) => ({
      name: `Outgoing bytes (${name})`,
      childName: `outgoing ${name}`,
    })),
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
                title={(datum: Datum) =>
                  format.dateTime(datum.x, {
                    timeZone: "UTC",
                    timeStyle: "medium",
                    dateStyle: "short",
                  })
                }
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
          tickCount={4}
          orientation={"bottom"}
          offsetY={padding.bottom}
        />
        <ChartAxis
          dependentAxis
          tickFormat={(d) => {
            return formatBytes(Math.abs(d), { maximumFractionDigits: 0 });
          }}
        />
        <ChartGroup>
          {Object.entries(incoming).map(([name, entries], idx) => {
            const entriesArray = Object.entries(entries ?? {});
            return (
              <ChartArea
                key={`incoming-line-${name}}`}
                data={entriesArray.map(([x, y]) => ({
                  name: `Incoming (${name})`,
                  x,
                  y,
                  value: y,
                }))}
                name={`incoming ${name}`}
                interpolation={"stepAfter"}
              />
            );
          })}
          {Object.entries(outgoing).map(([name, entries], idx) => {
            const entriesArray = Object.entries(entries ?? {});
            const incomingArray = Object.keys(incoming[name] ?? {});
            return (
              <ChartArea
                key={`outgoing-line-${name}}`}
                data={entriesArray.map(([x, y], idx) => ({
                  name: `Outgoing (${name})`,
                  x: incomingArray[idx],
                  y: -1 * y,
                  value: y,
                }))}
                name={`outgoing ${name}`}
                interpolation={"stepAfter"}
              />
            );
          })}
        </ChartGroup>
      </Chart>
    </div>
  );
}
