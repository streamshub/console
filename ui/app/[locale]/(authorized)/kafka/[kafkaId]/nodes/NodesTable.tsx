import {
  BrokerStatus,
  NodeRoles,
  NodeList,
  KafkaNode,
  ControllerStatus,
  NodePoolsType,
  Statuses,
} from "@/api/nodes/schema";
import {
  ClipboardCopy,
  Flex,
  FlexItem,
  Label,
  TextContent,
  Tooltip,
  Text,
  Icon,
  Level,
  LevelItem,
  Card,
  CardBody,
  CardTitle,
  CardHeader,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
  InProgressIcon,
  NewProcessIcon,
  PendingIcon,
} from "@/libs/patternfly/react-icons";
import { useFormatter, useTranslations } from "next-intl";
import Link from "next/link";
import { ReactNode } from "react";
import { Number } from "@/components/Format/Number";
import { ChartDonutUtilization } from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/useFormatBytes";
import { TableView, TableViewProps } from "@/components/Table/TableView";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";

export const NodeListColumns = [
  "id",
  "roles",
  "status",
  "replicas",
  "rack",
  "nodePool",
] as const;

export type NodeListColumn = (typeof NodeListColumns)[number];

const NodeRoleLabel: Record<NodeRoles, { label: ReactNode }> = {
  broker: { label: <>Broker</> },
  controller: { label: <>Controller</> },
};

const BrokerStatusLabel: Record<BrokerStatus, ReactNode> = {
  Running: (
    <>
      <Icon status={"success"}>
        <CheckCircleIcon />
      </Icon>
      &nbsp;Running
    </>
  ),
  Starting: (
    <>
      <Icon>
        <InProgressIcon />
      </Icon>
      &nbsp;Starting
    </>
  ),
  ShuttingDown: (
    <>
      <Icon>
        <PendingIcon />
      </Icon>
      &nbsp;Shutting Down
    </>
  ),
  PendingControlledShutdown: (
    <>
      <Icon status={"warning"}>
        <ExclamationTriangleIcon />
      </Icon>
      &nbsp;Pending Controlled Shutdown
    </>
  ),
  Recovery: (
    <>
      <Icon>
        <NewProcessIcon />
      </Icon>
      &nbsp;Recovery
    </>
  ),
  NotRunning: (
    <>
      <Icon status={"danger"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Not Running
    </>
  ),
  Unknown: (
    <>
      <Icon status={"danger"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Unknown
    </>
  ),
};

const ControllerStatusLabel: Record<ControllerStatus, ReactNode> = {
  QuorumLeader: (
    <>
      <Icon status={"success"}>
        <CheckCircleIcon />
      </Icon>
      &nbsp;Quorum Leader
    </>
  ),
  QuorumFollower: (
    <>
      <Icon status={"success"}>
        <CheckCircleIcon />
      </Icon>
      &nbsp;Quorum Follower
    </>
  ),
  QuorumFollowerLagged: (
    <>
      {" "}
      <Icon status={"warning"}>
        <ExclamationTriangleIcon />
      </Icon>
      &nbsp;Quorum Follower Lagged
    </>
  ),
  Unknown: (
    <>
      <Icon status={"danger"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Unknown
    </>
  ),
};

export type NodesTableProps = {
  nodeList: KafkaNode[] | undefined;
  nodesCount: number;
  page: number;
  perPage: number;
  filterNodePool: string[] | undefined;
  filterBrokerStatus: BrokerStatus[] | undefined;
  filterControllerStatus: ControllerStatus[] | undefined;
  filterRole: NodeRoles[] | undefined;
  onFilterNodePoolChange: (nodePool: string[] | undefined) => void;
  onFilterStatusChange: (
    brokerStatus: BrokerStatus[] | undefined,
    controllerStatus: ControllerStatus[] | undefined,
  ) => void;
  onFilterRoleChange: (role: NodeRoles[] | undefined) => void;
  nodePoolList: NodePoolsType | undefined;
  statuses: Statuses | undefined;
} & Pick<
  TableViewProps<NodeList, (typeof NodeListColumns)[number]>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>;

export function NodesTable({
  isColumnSortable,
  nodeList,
  filterNodePool,
  filterBrokerStatus,
  filterControllerStatus,
  filterRole,
  onFilterNodePoolChange,
  onFilterRoleChange,
  onFilterStatusChange,
  page,
  perPage,
  onPageChange,
  onClearAllFilters,
  nodesCount,
  nodePoolList,
  statuses,
}: NodesTableProps) {
  const t = useTranslations();
  const format = useFormatter();
  const formatBytes = useFormatBytes();

  const getBrokerStatusLabel = (statuses?: Record<string, number>) => {
    return Object.entries(BrokerStatusLabel).reduce(
      (acc, [key, label]) => {
        const count = statuses?.[key] ?? 0;
        acc[key as BrokerStatus] = (
          <Level>
            <LevelItem>{label}</LevelItem>
            <LevelItem>
              <span style={{ color: "var(--pf-v5-global--Color--200)" }}>
                {count}
              </span>
            </LevelItem>
          </Level>
        );
        return acc;
      },
      {} as Record<BrokerStatus, ReactNode>,
    );
  };

  const getControllerStatusLabel = (statuses?: Record<string, number>) => {
    return Object.entries(ControllerStatusLabel).reduce(
      (acc, [key, label]) => {
        const count = statuses?.[key] ?? 0;
        acc[key as ControllerStatus] = (
          <Level>
            <LevelItem>{label}</LevelItem>
            <LevelItem>
              <span style={{ color: "var(--pf-v5-global--Color--200)" }}>
                {count}
              </span>
            </LevelItem>
          </Level>
        );
        return acc;
      },
      {} as Record<ControllerStatus, ReactNode>,
    );
  };

  const nodePoolOptions = nodePoolList
    ? Object.fromEntries(
        Object.entries(nodePoolList).map(([poolName, roles]) => [
          poolName,
          {
            label: <>{poolName}</>,
            description: <>Nodes role: {roles.join(", ")}</>,
          },
        ]),
      )
    : {};

  const brokerStatusKeys = Object.keys(BrokerStatusLabel) as BrokerStatus[];
  const controllerStatusKeys = Object.keys(
    ControllerStatusLabel,
  ) as ControllerStatus[];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="pf-v5-u-pl-md">{t("nodes.title")}</CardTitle>
      </CardHeader>
      <CardBody>
        <TableView
          page={page}
          perPage={perPage}
          onPageChange={onPageChange}
          itemCount={nodesCount}
          isColumnSortable={isColumnSortable}
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
                  <Th key={key} width={20}>
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
                    {row.attributes.roles?.map((role, _) => {
                      return <div key={role}>{NodeRoleLabel[role].label}</div>;
                    })}
                  </Td>
                );
              case "status":
                return (
                  <Td key={key} dataLabel={"Status"}>
                    <div>
                      {row.attributes.broker &&
                        BrokerStatusLabel[row.attributes.broker.status]}
                    </div>
                    <div>
                      {row.attributes.controller &&
                        ControllerStatusLabel[row.attributes.controller.status]}
                    </div>
                  </Td>
                );
              case "replicas":
                return (
                  <Td key={key} dataLabel={"Total replicas"}>
                    <Number
                      value={
                        typeof row.attributes.broker?.leaderCount ===
                          "number" &&
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
                    {row.attributes.nodePool
                      ? (nodePoolOptions[row.attributes.nodePool]?.label ??
                        "n/a")
                      : "n/a"}
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
                <FlexItem
                  flex={{ default: "flex_1" }}
                  style={{ maxWidth: "50%" }}
                >
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
                            {
                              name: `Used capacity: ${formatBytes(diskUsage!)}`,
                            },
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
              onToggle: (nodePool) => {
                const newPool = filterNodePool?.includes(nodePool)
                  ? filterNodePool.filter((p) => p !== nodePool)
                  : [...filterNodePool!, nodePool];
                onFilterNodePoolChange(newPool);
              },
              onRemoveChip: (nodePool) => {
                const newPool = (filterNodePool || []).filter(
                  (p) => p !== nodePool,
                );
                onFilterNodePoolChange(newPool);
              },
              onRemoveGroup: () => {
                onFilterNodePoolChange(undefined);
              },
              options: nodePoolOptions,
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
              chips: [
                ...(filterBrokerStatus || []),
                ...(filterControllerStatus || []),
              ],
              onToggle: (status: BrokerStatus | ControllerStatus) => {
                const updateStatus = (statusList: any[], status: any) =>
                  statusList.includes(status)
                    ? statusList.filter((s) => s !== status)
                    : [...statusList, status];

                onFilterStatusChange(
                  brokerStatusKeys.includes(status as BrokerStatus)
                    ? updateStatus(filterBrokerStatus || [], status)
                    : filterBrokerStatus,
                  controllerStatusKeys.includes(status as ControllerStatus)
                    ? updateStatus(filterControllerStatus || [], status)
                    : filterControllerStatus,
                );
              },
              onRemoveChip: (status: BrokerStatus | ControllerStatus) => {
                onFilterStatusChange(
                  brokerStatusKeys.includes(status as BrokerStatus)
                    ? (filterBrokerStatus || []).filter((s) => s !== status)
                    : filterBrokerStatus,
                  controllerStatusKeys.includes(status as ControllerStatus)
                    ? (filterControllerStatus || []).filter((s) => s !== status)
                    : filterControllerStatus,
                );
              },
              onRemoveGroup: () => onFilterStatusChange(undefined, undefined),
              options: [
                {
                  groupLabel: "Broker",
                  groupOptions: getBrokerStatusLabel(statuses?.brokers),
                },
                {
                  groupLabel: "Controller",
                  groupOptions: getControllerStatusLabel(statuses?.controllers),
                },
              ],
            },
          }}
        />
      </CardBody>
    </Card>
  );
}
