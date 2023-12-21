"use client";

import { useChartWidth } from "@/app/[locale]/kafka/[kafkaId]/overview/useChartWidth";
import {
  Chart,
  ChartAxis,
  ChartBar,
  ChartLegend,
  ChartStack,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  ToggleGroup,
  ToggleGroupItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useState } from "react";

export function DistributionChart({
  data,
}: {
  data: Record<string, { leaders: number; followers: number }>;
}) {
  const [containerRef, width] = useChartWidth();
  const [includeLeaders, setIncludeLeaders] = useState(true);
  const [includeFollowers, setIncludeFollowers] = useState(true);
  const label =
    includeFollowers || includeLeaders
      ? includeLeaders && includeFollowers
        ? "total partitions"
        : includeLeaders
          ? "leaders"
          : "followers"
      : "total partitions";
  return (
    <Card className={"pf-v5-u-mb-lg"}>
      <CardHeader>
        <CardTitle>
          Partitions distribution (% of total){" "}
          <Tooltip
            content={
              "The percentage distribution of partitions across brokers in the cluster. Consider rebalancing if the distribution is uneven to ensure efficient resource utilization."
            }
          >
            <HelpIcon />
          </Tooltip>
        </CardTitle>
      </CardHeader>
      <CardBody>
        <ToggleGroup isCompact aria-label="Compact variant toggle group">
          <ToggleGroupItem
            text={`Leaders (${Object.values(data).reduce(
              (acc, v) => v.leaders + acc,
              0,
            )})`}
            buttonId="toggle-group-compact-1"
            isSelected={includeLeaders}
            onChange={() => setIncludeLeaders((v) => !v)}
          />
          <ToggleGroupItem
            text={`Followers (${Object.values(data).reduce(
              (acc, v) => v.followers + acc,
              0,
            )})`}
            buttonId="toggle-group-compact-2"
            isSelected={includeFollowers}
            onChange={() => setIncludeFollowers((v) => !v)}
          />
        </ToggleGroup>
        <div ref={containerRef}>
          <Chart
            ariaDesc="Average number of pets"
            ariaTitle="Stack chart example"
            containerComponent={
              <ChartVoronoiContainer
                labels={({ datum }) => `${datum.name} ${label}: ${datum.y}`}
                constrainToVisibleArea
              />
            }
            legendOrientation="horizontal"
            legendPosition="bottom"
            legendComponent={
              <ChartLegend
                orientation={"horizontal"}
                data={Object.keys(data).flatMap((node) => [
                  {
                    name: `Broker ${node} ${label}`,
                  },
                ])}
                itemsPerRow={width > 600 ? 3 : 1}
              />
            }
            height={100}
            padding={{
              bottom: 70,
              left: 0,
              right: 0, // Adjusted to accommodate legend
              top: 30,
            }}
            width={width}
          >
            <ChartAxis
              style={{
                axis: { stroke: "transparent" },
                ticks: { stroke: "transparent" },
                tickLabels: { fill: "transparent" },
              }}
            />
            <ChartStack>
              {Object.entries(data).map(([node, data], idx) => (
                <ChartBar
                  key={idx}
                  horizontal={true}
                  barWidth={15}
                  data={[
                    {
                      name: `Broker ${node}`,
                      x: "x",
                      y:
                        includeFollowers || includeLeaders
                          ? includeLeaders && includeFollowers
                            ? data.leaders + data.followers
                            : includeLeaders
                              ? data.leaders
                              : data.followers
                          : data.leaders + data.followers,
                    },
                  ]}
                />
              ))}
            </ChartStack>
          </Chart>
        </div>
      </CardBody>
    </Card>
  );
}
