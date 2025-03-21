import {
  BrokerStatus,
  NodePools,
  NodeRoles,
  NodeList,
  KafkaNode,
  ControllerStatus,
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
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";

function capitalizeWords(value: string) {
  if (typeof value !== "string") {
    return value; // Return non-string values unchanged
  }
  return value.replace(/\b\w/g, (char) => char.toUpperCase());
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

const NodePoolLabel: Record<
  NodePools,
  { label: ReactNode; description: ReactNode }
> = {
  dual1: {
    label: <>both-pool</>,
    description: <>Nodes role: broker, controller</>,
  },
  brokers1: {
    label: <>B- pool - 1</>,
    description: <>Nodes role: broker</>,
  },
  controllers1: {
    label: <>C-pool-1</>,
    description: <>Nodes role: controller</>,
  },
};

const NodeRoleLabel: Record<NodeRoles, { label: ReactNode }> = {
  broker: { label: <>Broker</> },
  controller: { label: <>Controller</> },
};

const BrokerStatusLabel: Record<BrokerStatus, ReactNode> = {
  NotRunning: (
    <>
      <Icon status={"warning"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Not Running
    </>
  ),
  PendingControlledShutdown: (
    <>
      <Icon status={"danger"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Pending Controlled Shutdown
    </>
  ),
  ShuttingDown: (
    <>
      <Icon status={"warning"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Shutting Down
    </>
  ),
  Recovery: (
    <>
      <Icon status={"warning"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Recovery
    </>
  ),
  Starting: (
    <>
      <Icon status={"warning"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Starting
    </>
  ),
  Running: (
    <>
      <Icon status={"success"}>
        <CheckCircleIcon />
      </Icon>
      &nbsp;Running
    </>
  ),
  Unknown: (
    <>
      <Icon status={"warning"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Unknown
    </>
  ),
};

const ControllerStatusLabel: Record<ControllerStatus, ReactNode> = {
  QuorumLeader: <>Quorum Leader</>,
  QuorumFollowerLagged: <>Quorum Follower Lagged</>,
  QuorumFollower: <>Quorum Follower</>,
  Unknown: <>Unknown</>,
};

export type NodesTableProps = {
  nodeList: KafkaNode[] | undefined;
  page: number;
  perPage: number;
  filterNodePool: NodePools[] | undefined;
  filterStatus: (BrokerStatus | ControllerStatus)[] | undefined;
  filterRole: NodeRoles[] | undefined;
  onFilterNodePoolChange: (pool: NodePools[] | undefined) => void;
  onFilterStatusChange: (
    status: (BrokerStatus | ControllerStatus)[] | undefined,
  ) => void;
  onFilterRoleChange: (role: NodeRoles[] | undefined) => void;
} & Pick<
  TableViewProps<NodeList, (typeof NodeListColumns)[number]>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>;

export function NodesTable({
  isColumnSortable,
  nodeList,
  filterNodePool,
  filterStatus,
  filterRole,
  onFilterNodePoolChange,
  onFilterRoleChange,
  onFilterStatusChange,
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
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
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
                  return <div key={role}>{NodeRoleLabel[role].label}</div>;
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
                    {capitalizeWords(row.attributes.controller.status)}
                  </div>
                )}
                {row.attributes.broker &&
                  BrokerStatusLabel[row.attributes.broker.status]}
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
            onFilterRoleChange(newRole);
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
        Status: {
          type: "groupedCheckbox",
          chips: filterStatus || [],
          onToggle: (status: BrokerStatus | ControllerStatus) => {
            const newStatus = filterStatus?.includes(status)
              ? filterStatus.filter((s) => s !== status)
              : [...filterStatus!, status];
            onFilterStatusChange(newStatus);
          },
          onRemoveChip: (status: BrokerStatus | ControllerStatus) => {
            const newStatus = (filterStatus || []).filter((s) => s !== status);
            onFilterStatusChange(newStatus);
          },
          onRemoveGroup: () => {
            onFilterStatusChange(undefined);
          },
          options: [
            {
              groupLabel: "Broker Status",
              groupOptions: BrokerStatusLabel,
            },
            {
              groupLabel: "Controller Status",
              groupOptions: ControllerStatusLabel,
            },
          ],
        },
      }}
    />
  );
}
