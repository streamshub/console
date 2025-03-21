import {
  BrokerStatus,
  NodePools,
  NodeRoles,
  NodeList,
  KafkaNode,
} from "@/api/nodes/schema";
import {
  ClipboardCopy,
  Flex,
  FlexItem,
  Label,
  TextContent,
  Tooltip,
  Text,
  SelectOption,
  Icon,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  HelpIcon,
} from "@/libs/patternfly/react-icons";
import { useFormatter, useTranslations } from "next-intl";
import Link from "next/link";
import { ReactNode } from "react";
import { Number } from "@/components/Format/Number";
import { ChartDonutUtilization } from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/useFormatBytes";
import { TableView, TableViewProps } from "@/components/Table/TableView";

function splitAndCapitalize(value: string) {
  // Split by uppercase letters, preserving them with a lookahead
  const words = value.split(/(?=[A-Z])/);

  // Capitalize each word and join with a space
  return words
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export const NodeListColumns = [
  "id",
  "roles",
  "status",
  "replicas",
  "rack",
  "nodePool",
] as const;

export type NodeListColumn = (typeof NodeListColumns)[number];

const NodePoolLabel: Record<NodePools, ReactNode> = {
  dual1: (
    <SelectOption
      value="both-pool"
      description="Nodes role: broker, controller"
    >
      both-pool
    </SelectOption>
  ),
  brokers1: (
    <SelectOption value="B-pool-1" description="Nodes role: broker">
      B-pool-1
    </SelectOption>
  ),
  controllers1: (
    <SelectOption value="C-pool-1" description="Nodes role: controller">
      C-pool-1
    </SelectOption>
  ),
};

const NodeRoleLabel: Record<NodeRoles, ReactNode> = {
  broker: <>Broker</>,
  controller: <>Controller</>,
};

export type NodesTableProps = {
  nodeList: KafkaNode[] | undefined;
  page: number;
  perPage: number;
  filterNodePool: NodePools[] | undefined;
  // filterStatus: BrokerStatus[] | undefined;
  filterRole: NodeRoles[] | undefined;
  onFilterNodePoolChange: (pool: NodePools[] | undefined) => void;
  // onFilterStatusChange: (status: BrokerStatus[] | undefined) => void;
  onFilterRoleChange: (role: NodeRoles[] | undefined) => void;
} & Pick<
  TableViewProps<NodeList, (typeof NodeListColumns)[number]>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>;

export function NodesTable({
  isColumnSortable,
  nodeList,
  filterNodePool,
  // filterStatus,
  filterRole,
  onFilterNodePoolChange,
  onFilterRoleChange,
  // onFilterStatusChange,
  page,
  perPage,
  onPageChange,
  onClearAllFilters,
}: NodesTableProps) {
  const t = useTranslations();
  const format = useFormatter();
  const formatBytes = useFormatBytes();

  return (
    <TableView
      page={page}
      perPage={perPage}
      onPageChange={onPageChange}
      isRowExpandable={() => true}
      onClearAllFilters={onClearAllFilters}
      data={nodeList}
      emptyStateNoData={<></>}
      emptyStateNoResults={<></>}
      ariaLabel={"Kafka nodes"}
      columns={NodeListColumns}
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
                <Tooltip content={t("nodes.replicas_tooltip")}>
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "rack":
            return (
              <Th key={key}>
                {t("nodes.rack")}{" "}
                <Tooltip content={t("nodes.rack_tooltip")}>
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
                {row.attributes.metadataState?.status === "leader" && (
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
                {row.attributes.roles.map((role, _) => {
                  return <div key={role}>{role}</div>;
                })}
              </Td>
            );
          case "status":
            return (
              <Td key={key} dataLabel={"Status"}>
                {row.attributes.controller && (
                  <div>
                    <Icon
                      status={
                        row.attributes.controller.status !==
                        "QuorumFollowerLagged"
                          ? "success"
                          : "warning"
                      }
                    >
                      {row.attributes.controller.status !==
                      "QuorumFollowerLagged" ? (
                        <CheckCircleIcon />
                      ) : (
                        <ExclamationCircleIcon />
                      )}
                    </Icon>
                    &nbsp;
                    {splitAndCapitalize(row.attributes.controller.status)}
                  </div>
                )}
                {row.attributes.broker && (
                  <div>
                    <Icon
                      status={
                        row.attributes.broker.status === "Running"
                          ? "success"
                          : "warning"
                      }
                    >
                      {row.attributes.broker.status === "Running" ? (
                        <CheckCircleIcon />
                      ) : (
                        <ExclamationCircleIcon />
                      )}
                    </Icon>
                    &nbsp;
                    {splitAndCapitalize(row.attributes.broker.status)}
                  </div>
                )}
              </Td>
            );
          case "replicas":
            return (
              <Td key={key} dataLabel={"Total replicas"}>
                <Number
                  value={
                    typeof row.attributes.broker?.leaderCount === "number" &&
                    typeof row.attributes.broker?.replicaCount === "number"
                      ? row.attributes.broker.leaderCount +
                        row.attributes.broker.replicaCount
                      : undefined
                  }
                />
              </Td>
            );
          case "rack":
            return (
              <Td key={key} dataLabel={"Rack"}>
                {row.attributes.rack || "n/a"}
              </Td>
            );
          case "nodePool":
            return (
              <Td key={key} dataLabel={"Node Pool"}>
                {row.attributes.nodePool ?? "n/a"}
              </Td>
            );
        }
      }}
      getExpandedRow={({ row }) => {
        const diskCapacity = row.attributes.storageCapacity ?? undefined;
        const diskUsage = row.attributes.storageUsed ?? undefined;

        let usedCapacity =
          diskUsage !== undefined && diskCapacity !== undefined
            ? diskUsage / diskCapacity
            : undefined;
        return (
          <Flex gap={{ default: "gap4xl" }} className={"pf-v5-u-p-xl"}>
            <FlexItem flex={{ default: "flex_1" }} style={{ maxWidth: "50%" }}>
              <TextContent>
                <Text>{t.rich("nodes.host_name")}</Text>
                <Text>
                  <ClipboardCopy
                    isReadOnly={true}
                    variant={"expansion"}
                    isExpanded={true}
                  >
                    {row.attributes.host || "n/a"}
                  </ClipboardCopy>
                </Text>
              </TextContent>
            </FlexItem>
            <FlexItem>
              <TextContent>
                <Text>{t.rich("nodes.disk_usage")}</Text>
              </TextContent>
              <div>
                {usedCapacity !== undefined && (
                  <div style={{ height: "300px", width: "230px" }}>
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
                        { name: `Used capacity: ${formatBytes(diskUsage!)}` },
                        {
                          name: `Available: ${formatBytes(diskCapacity! - diskUsage!)}`,
                        },
                      ]}
                      legendOrientation="vertical"
                      legendPosition="bottom"
                      padding={{
                        bottom: 75, // Adjusted to accommodate legend
                        left: 20,
                        right: 20,
                        top: 20,
                      }}
                      title={`${format.number(usedCapacity, {
                        style: "percent",
                      })}`}
                      subTitle={`of ${formatBytes(diskCapacity!)}`}
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
                <Text>{t.rich("nodes.kafka_version")}</Text>
              </TextContent>
              <div>{row.attributes.kafkaVersion ?? "Unknown"}</div>
            </FlexItem>
          </Flex>
        );
      }}
      filters={{
        "Node pool": {
          type: "checkbox",
          chips: filterNodePool || [],
          onToggle: (pool) => {
            const newPool = filterNodePool?.includes(pool)
              ? filterNodePool.filter((p) => p !== pool)
              : [...filterNodePool!, pool];
            onFilterNodePoolChange(newPool);
          },
          onRemoveChip: (pool) => {
            const newPool = (filterNodePool || []).filter((p) => p !== pool);
            onFilterNodePoolChange(newPool);
          },
          onRemoveGroup: () => {
            onFilterNodePoolChange(undefined);
          },
          options: NodePoolLabel,
        },
        Role: {
          type: "checkbox",
          chips: filterRole || [],
          onToggle: (role) => {
            const newRole = filterRole?.includes(role)
              ? filterRole.filter((r) => r !== role)
              : [...filterRole!, role];
            onFilterNodePoolChange(newRole);
          },
          onRemoveChip: (role) => {
            const newRole = (filterRole || []).filter((r) => r !== role);
            onFilterRoleChange(newRole);
          },
          onRemoveGroup: () => {
            onFilterRoleChange(undefined);
          },
          options: NodeRoleLabel,
        },
      }}
    />
  );
}
