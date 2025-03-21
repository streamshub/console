"use client";

import { useChartWidth } from "@/components/ClusterOverview/components/useChartWidth";
import {
  Chart,
  ChartAxis,
  ChartBar,
  ChartStack,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Icon,
  ToggleGroup,
  ToggleGroupItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
} from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useState } from "react";

export function DistributionChart({
  data,
  nodesCount,
}: {
  data: Record<string, { leaders?: number; followers?: number }>;
  nodesCount: {
    totalNodes: number;
    totalBrokers: number;
    totalControllers: number;
    leadControllerId: string;
  };
}) {
  const t = useTranslations();
  const [containerRef, width] = useChartWidth();
  const [filter, setFilter] = useState<"all" | "leaders" | "followers">("all");

  const leadController = nodesCount.leadControllerId;

  const allCount = Object.values(data).reduce(
    (acc, v) => (v.followers ?? 0) + (v.leaders ?? 0) + acc,
    0,
  );
  const leadersCount = Object.values(data).reduce(
    (acc, v) => (v.leaders ?? 0) + acc,
    0,
  );
  const followersCount = Object.values(data).reduce(
    (acc, v) => (v.followers ?? 0) + acc,
    0,
  );

  const getCount = (nodeData: { leaders?: number; followers?: number }) => {
    switch (filter) {
      case "leaders":
        return nodeData.leaders;
      case "followers":
        return nodeData.followers;
      default:
        return typeof nodeData.leaders == "number" &&
          typeof nodeData.followers == "number"
          ? nodeData.leaders + nodeData.followers
          : undefined;
    }
  };

  const getPercentage = (count: number) => {
    switch (filter) {
      case "leaders":
        return ((count / leadersCount) * 100).toFixed(2);
      case "followers":
        return ((count / followersCount) * 100).toFixed(2);
      default:
        return ((count / allCount) * 100).toFixed(2);
    }
  };

  return (
    <Grid hasGutter>
      <GridItem md={3}>
        <Card>
          <CardBody>
            <DescriptionList isCompact isHorizontal>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  {t("DistributionChart.total_nodes")}{" "}
                  <Tooltip content={t("DistributionChart.total_nodes_tooltip")}>
                    <HelpIcon />
                  </Tooltip>
                </DescriptionListTerm>
                <DescriptionListDescription>
                  {nodesCount.totalNodes}
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  {t("DistributionChart.controller_role")}
                </DescriptionListTerm>
                <DescriptionListDescription>
                  <Icon status={"warning"}>
                    <ExclamationTriangleIcon />
                  </Icon>
                  &nbsp; {nodesCount.totalControllers}
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  {t("DistributionChart.broker_role")}
                </DescriptionListTerm>
                <DescriptionListDescription>
                  <Icon status={"success"}>
                    <CheckCircleIcon />
                  </Icon>
                  &nbsp; {nodesCount.totalBrokers}
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  {t("DistributionChart.lead_controller")}{" "}
                  <Tooltip
                    content={t("DistributionChart.lead_controller_tooltip")}
                  >
                    <HelpIcon />
                  </Tooltip>
                </DescriptionListTerm>
                <DescriptionListDescription>
                  <Link href={`nodes/${leadController}`}>
                    {t("DistributionChart.lead_controller_value", {
                      leadController,
                    })}
                  </Link>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </DescriptionList>
          </CardBody>
        </Card>
      </GridItem>
      <GridItem md={9}>
        <Card className={"pf-v5-u-mb-lg"}>
          <CardHeader>
            <CardTitle>
              {t("DistributionChart.partitions_distribution_of_total")}{" "}
              <Tooltip
                content={t(
                  "DistributionChart.partitions_distribution_of_total_tooltip",
                )}
              >
                <HelpIcon />
              </Tooltip>
            </CardTitle>
          </CardHeader>
          {allCount > 0 ? (
            <CardBody>
              <ToggleGroup
                isCompact
                aria-label={t("DistributionChart.distribution_toggles")}
              >
                <ToggleGroupItem
                  text={t("DistributionChart.all_label", {
                    count: allCount,
                  })}
                  buttonId="toggle-group-compact-0"
                  isSelected={filter == "all"}
                  onChange={() => setFilter("all")}
                />
                <ToggleGroupItem
                  text={t("DistributionChart.leaders_label", {
                    count: leadersCount,
                  })}
                  buttonId="toggle-group-compact-1"
                  isSelected={filter == "leaders"}
                  onChange={() => setFilter("leaders")}
                />
                <ToggleGroupItem
                  text={t("DistributionChart.followers_label", {
                    count: followersCount,
                  })}
                  buttonId="toggle-group-compact-2"
                  isSelected={filter === "followers"}
                  onChange={() => setFilter("followers")}
                />
              </ToggleGroup>
              <div ref={containerRef}>
                <Chart
                  ariaDesc={t(
                    "DistributionChart.distribution_chart_description",
                  )}
                  ariaTitle={t("DistributionChart.distribution_chart_title")}
                  containerComponent={
                    <ChartVoronoiContainer
                      labels={({ datum }) => {
                        switch (filter) {
                          case "followers":
                            return t(
                              "DistributionChart.broker_node_voronoi_followers",
                              { name: datum.name, value: datum.y },
                            );
                          case "leaders":
                            return t(
                              "DistributionChart.broker_node_voronoi_leaders",
                              { name: datum.name, value: datum.y },
                            );
                          default:
                            return t(
                              "DistributionChart.broker_node_voronoi_all",
                              {
                                name: datum.name,
                                value: datum.y,
                              },
                            );
                        }
                      }}
                      constrainToVisibleArea
                    />
                  }
                  legendOrientation="horizontal"
                  legendPosition="bottom"
                  legendData={Object.keys(data).map((node) => {
                    const count = getCount(data[node]);
                    if (count !== undefined) {
                      const percentage = getPercentage(count);
                      return {
                        name: t("DistributionChart.broker_node_count", {
                          node,
                          count,
                          percentage,
                        }),
                      };
                    }
                    return {
                      name: t("DistributionChart.broker_node_count_missing", {
                        node,
                      }),
                    };
                  })}
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
                            y: getCount(data) ?? 0,
                          },
                        ]}
                      />
                    ))}
                  </ChartStack>
                </Chart>
              </div>
            </CardBody>
          ) : (
            <CardBody>
              <div>{t("DistributionChart.metrics_unavailable")}</div>
            </CardBody>
          )}
        </Card>
      </GridItem>
    </Grid>
  );
}
