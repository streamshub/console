"use client";

import { Number } from "@/components/Format/Number";
import { ResponsiveTable } from "@/components/Table";
import {
  ChartDonutUtilization,
} from "@/libs/patternfly/react-charts";
import {
  ClipboardCopy,
  Flex,
  FlexItem,
  Label,
  Text,
  TextContent,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useFormatBytes } from "@/utils/useFormatBytes";
import { Icon } from "@patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
} from "@patternfly/react-icons";
import { useFormatter } from "next-intl";
import Link from "next/link";
import { useTranslations } from "next-intl";

const columns = ["id", "roles", "status", "replicas", "rack", "nodePool"] as const;

export type NodeStatus = {
  stable: boolean,
  description: string,
};

export type Node = {
  id: string;
  nodePool?: string;
  roles: string[];
  isLeader: boolean;
  brokerStatus?: NodeStatus;
  controllerStatus?: NodeStatus;
  followers?: number;
  leaders?: number;
  rack?: string;
  hostname?: string;
  diskCapacity?: number;
  diskUsage?: number;
  kafkaVersion?: string;
};

export function NodesTable({ nodes }: { nodes: Node[] }) {

  const t = useTranslations();
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  return (
    <ResponsiveTable
      ariaLabel={"Kafka nodes"}
      columns={columns}
      data={nodes}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "id":
            return <Th key={key}>{t("nodes.broker_id")}</Th>;
          case "roles":
            return <Th key={key}>{t("nodes.roles")}</Th>;
          case "status":
            return <Th key={key}>{t("nodes.status")}</Th>;
          case "replicas":
            return (
              <Th key={key}>
                {t("nodes.replicas")}{" "}
                <Tooltip
                  content={
                    t("nodes.replicas_tooltip")
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "rack":
            return (
              <Th key={key}>
                {t("nodes.rack")}{" "}
                <Tooltip
                  content={
                    t("nodes.rack_tooltip")
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "nodePool":
            return <Th key={key}>{t("nodes.nodePool")}</Th>;
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "id":
            return (
              <Td key={key} dataLabel={"Node ID"}>
                <Link href={`nodes/${row.id}`}>{row.id}</Link>
                {row.isLeader && (
                  <Label
                      isCompact={true}
                      color={"green"}
                      className={"pf-v5-u-ml-sm"}
                    >
                    {t.rich("nodes.lead_controller")}
                  </Label>
                )}
              </Td>
            );
          case "roles":
            return (
              <Td key={key} dataLabel={"Roles"}>
                {
                    row.roles.map((role, _) => {
                        return <div key={role}>{role}</div>;
                    })
                }
              </Td>
            );
          case "status":
            return (
              <Td key={key} dataLabel={"Status"}>
                { row.controllerStatus &&
                  <div>
                  <Icon status={row.controllerStatus.stable ? "success" : "warning"}>
                    {row.controllerStatus.stable ? <CheckCircleIcon /> : <ExclamationCircleIcon />}
                  </Icon>
                  &nbsp;
                  {row.controllerStatus?.description}
                  </div>
                }
                { row.brokerStatus &&
                  <div>
                  <Icon status={row.brokerStatus.stable ? "success" : "warning"}>
                    {row.brokerStatus.stable ? <CheckCircleIcon /> : <ExclamationCircleIcon />}
                  </Icon>
                  &nbsp;
                  {row.brokerStatus?.description}
                  </div>
                }
              </Td>
            );
          case "replicas":
            return (
              <Td key={key} dataLabel={"Total replicas"}>
                <Number
                  value={
                    typeof row.followers == 'number' && typeof row.leaders == 'number'
                      ? row.followers + row.leaders
                      : undefined
                  }
                />
              </Td>
            );
          case "rack":
            return (
              <Td key={key} dataLabel={"Rack"}>
                {row.rack || "n/a"}
              </Td>
            );
          case "nodePool":
            return (
              <Td key={key} dataLabel={"Node Pool"}>
                {row.nodePool ?? "n/a"}
              </Td>
            );
        }
      }}
      isRowExpandable={() => true}
      getExpandedRow={({ row }) => {
        let usedCapacity = (row.diskUsage !== undefined && row.diskCapacity !== undefined)
          ? (row.diskUsage / row.diskCapacity) : undefined;

        return (
          <Flex gap={{ default: "gap4xl" }} className={"pf-v5-u-p-xl"}>
            <FlexItem flex={{ default: "flex_1" }} style={{ maxWidth: "50%" }}>
              <TextContent>
                <Text>
                  {t.rich("nodes.host_name")}
                </Text>
                <Text>
                  <ClipboardCopy
                    isReadOnly={true}
                    variant={"expansion"}
                    isExpanded={true}
                  >
                    {row.hostname || "n/a"}
                  </ClipboardCopy>
                </Text>
              </TextContent>
            </FlexItem>
            <FlexItem>
              <TextContent>
                <Text>
                  {t.rich("nodes.disk_usage")}
                </Text>
              </TextContent>
              <div>
                {usedCapacity !== undefined && (
                    <div style={{ height: '300px', width: '230px' }}>
                    <ChartDonutUtilization
                        data={{
                          x: "Used capacity",
                          y: usedCapacity * 100,
                        }}
                        labels={({ datum }) =>
                          datum.x
                            ? `${datum.x}: ${format.number(datum.y / 100, {
                              style: "percent",
                            })}`
                            : null
                        }
                        legendData={[
                          { name: `Used capacity: ${formatBytes(row.diskUsage!)}` },
                          { name: `Available: ${formatBytes(row.diskCapacity! - row.diskUsage!)}` },
                        ]}
                        legendOrientation="vertical"
                        legendPosition="bottom"
                        padding={{
                            bottom: 75, // Adjusted to accommodate legend
                            left: 20,
                            right: 20,
                            top: 20
                          }}
                        title={`${format.number(usedCapacity, {
                            style: "percent",
                          },
                        )}`}
                        subTitle={`of ${formatBytes(row.diskCapacity!)}`}
                        thresholds={[{ value: 60 }, { value: 90 }]}
                        height={300}
                        width={230}
                      />
                    </div>
                  )}
              </div>
            </FlexItem>
            <FlexItem>
              <TextContent>
                <Text>
                  {t.rich("nodes.kafka_version")}
                </Text>
              </TextContent>
              <div>
                {row.kafkaVersion ?? "Unknown"}
              </div>
            </FlexItem>
          </Flex>
        );
      }}
    />
  );
}
