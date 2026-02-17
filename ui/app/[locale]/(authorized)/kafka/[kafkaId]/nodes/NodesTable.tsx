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
  Content,
  Tooltip,
  Card,
  CardBody,
  CardTitle,
  CardHeader,
  Level,
  LevelItem,
} from "@/libs/patternfly/react-core";
import { useFormatter, useTranslations } from "next-intl";
import Link from "next/link";
import { ReactNode } from "react";
import { Number } from "@/components/Format/Number";
import { ChartDonutUtilization } from "@/libs/patternfly/react-charts";
import { useFormatBytes } from "@/utils/useFormatBytes";
import { TableView, TableViewProps } from "@/components/Table/TableView";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import {
  BrokerLabel,
  ControllerLabel,
  getBrokerStatusLabel,
  getControllerStatusLabel,
  RoleLabel,
} from "./NodesLabel";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { Th } from "@/libs/patternfly/react-table";
import RichText from "@/components/RichText";

export const NodeListColumns = [
  "id",
  "roles",
  "status",
  "replicas",
  "rack",
  "nodePool",
] as const;

export type NodeListColumn = (typeof NodeListColumns)[number];

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
  "onPageChange" | "onClearAllFilters"
>;

export function NodesTable({
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

  const NodeRoleLabel = RoleLabel(statuses);
  const BrokerStatusLabel = BrokerLabel();
  const ControllerStatusLabel = ControllerLabel();
  const BrokerStatusLabelOptions = getBrokerStatusLabel(statuses?.brokers);
  const ControllerStatusLabelOptions = getControllerStatusLabel(
    statuses?.controllers,
  );

  const nodePoolOptions = nodePoolList
    ? Object.fromEntries(
        Object.entries(nodePoolList).map(([poolName, poolMeta]) => [
          poolName,
          {
            label: <>{poolName}</>, // Label without count
            labelWithCount: (
              <Flex>
                <FlexItem>{poolName}</FlexItem>
                <FlexItem
                  align={{ default: "alignRight" }}
                  style={{ color: "var(--pf-t--global--text--color--subtle)" }}
                >
                  {poolMeta.count}
                </FlexItem>
              </Flex>
            ),
            description: <>Nodes role: {poolMeta.roles.join(", ")}</>,
          },
        ]),
      )
    : {};

  const brokerStatusKeys = Object.keys(BrokerStatusLabel) as BrokerStatus[];
  const controllerStatusKeys = Object.keys(
    ControllerStatusLabel,
  ) as ControllerStatus[];

  return (
    <Card isFullHeight>
      <CardHeader>
        <CardTitle className="pf-v6-u-pl-md">{t("nodes.title")}</CardTitle>
      </CardHeader>
      <CardBody isFilled style={{ minHeight: "600px" }}>
        <TableView
          page={page}
          perPage={perPage}
          onPageChange={onPageChange}
          itemCount={nodesCount}
          isRowExpandable={() => true}
          isFiltered={
            filterNodePool?.length !== 0 ||
            filterRole?.length != 0 ||
            filterBrokerStatus?.length !== 0 ||
            filterControllerStatus?.length !== 0
          }
          onClearAllFilters={onClearAllFilters}
          data={nodeList}
          emptyStateNoData={<></>}
          emptyStateNoResults={
            <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
          }
          ariaLabel={"Kafka nodes"}
          columns={NodeListColumns}
          renderHeader={({ column, key }) => {
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
                    {row.attributes.broker ? (
                      <Link href={`nodes/${row.id}`}>{row.id}</Link>
                    ) : (
                      <>{row.id}</>
                    )}
                    {row.attributes.metadataState?.status === "leader" && (
                      <Label
                        isCompact={true}
                        color={"green"}
                        className={"pf-v6-u-ml-sm"}
                      >
                        <RichText>
                          {(tags) => t.rich("nodes.lead_controller", tags)}
                        </RichText>
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
                    <div className={"pf-v6-u-active-color-100"}>
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
              <Flex gap={{ default: "gap4xl" }} className={"pf-v6-u-p-xl"}>
                <FlexItem
                  flex={{ default: "flex_1" }}
                  style={{ maxWidth: "50%" }}
                >
                  <Content>
                    <Content>
                      <RichText>
                        {(tags) => t.rich("nodes.host_name", tags)}
                      </RichText>
                    </Content>
                    <Content>
                      <ClipboardCopy
                        isReadOnly={true}
                        variant={"expansion"}
                        isExpanded={true}
                      >
                        {row.attributes.host || "n/a"}
                      </ClipboardCopy>
                    </Content>
                  </Content>
                </FlexItem>
                <FlexItem>
                  <Content>
                    <Content>
                      <RichText>
                        {(tags) => t.rich("nodes.disk_usage", tags)}
                      </RichText>
                    </Content>
                  </Content>
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
                              name: `Available: ${formatBytes(
                                diskCapacity! - diskUsage!,
                              )}`,
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
                  <Content>
                    <Content>
                      <RichText>
                        {(tags) => t.rich("nodes.kafka_version", tags)}
                      </RichText>
                    </Content>
                  </Content>
                  <div>{row.attributes.kafkaVersion ?? "Unknown"}</div>
                </FlexItem>
              </Flex>
            );
          }}
          filters={{
            "Node pool": {
              type: "checkbox",
              placeholder: `${t("nodes.all_node_pools_placeholder")} (${
                nodePoolList ? Object.keys(nodePoolList).length : 0
              })`,
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
              options: nodePoolList
                ? Object.keys(nodePoolOptions).reduce(
                    (acc, poolName) => {
                      acc[poolName] = {
                        label: nodePoolOptions[poolName].labelWithCount,
                        description: nodePoolOptions[poolName].description,
                      };
                      return acc;
                    },
                    {} as Record<
                      string,
                      { label: ReactNode; description: ReactNode }
                    >,
                  )
                : {},
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
              options: Object.keys(NodeRoleLabel).reduce(
                (acc: Record<NodeRoles, { label: ReactNode }>, role) => {
                  acc[role as NodeRoles] = {
                    label: NodeRoleLabel[role as NodeRoles].labelWithCount,
                  };
                  return acc;
                },
                {} as Record<NodeRoles, { label: ReactNode }>,
              ),
            },
            Status: {
              type: "groupedCheckbox",
              placeholder: t("nodes.status_placeholder"),
              chips: [
                ...(filterBrokerStatus || []),
                ...(filterControllerStatus || []),
              ],
              onToggle: (status: BrokerStatus | ControllerStatus) => {
                const updateStatus = (statusList: any[], status: any) =>
                  statusList.includes(status)
                    ? statusList.filter((s) => s !== status)
                    : [...statusList, status];

                const newBrokerStatus = brokerStatusKeys.includes(
                  status as BrokerStatus,
                )
                  ? updateStatus(filterBrokerStatus || [], status)
                  : filterBrokerStatus;

                const newControllerStatus = controllerStatusKeys.includes(
                  status as ControllerStatus,
                )
                  ? updateStatus(filterControllerStatus || [], status)
                  : filterControllerStatus;

                onFilterStatusChange(newBrokerStatus, newControllerStatus);
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
                  groupOptions: BrokerStatusLabelOptions,
                },
                {
                  groupLabel: "Controller",
                  groupOptions: ControllerStatusLabelOptions,
                },
              ],
            },
          }}
        />
      </CardBody>
    </Card>
  );
}
