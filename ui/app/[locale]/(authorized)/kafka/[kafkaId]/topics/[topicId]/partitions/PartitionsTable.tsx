"use client";
import { getTopic } from "@/api/topics/actions";
import { PartitionStatus, Topic } from "@/api/topics/schema";
import { NoResultsEmptyState } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/[topicId]/partitions/NoResultsEmptyState";
import { Bytes } from "@/components/Format/Bytes";
import { TableView } from "@/components/Table";
import {
  Icon,
  Label,
  LabelGroup,
  PageSection,
  ToggleGroup,
  ToggleGroupItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  FlagIcon,
  HelpIcon,
} from "@/libs/patternfly/react-icons";
import {
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
} from "@/libs/patternfly/react-icons";
import { Th } from "@/libs/patternfly/react-table";
import { ReactNode, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { usePagination } from "@/utils/usePagination";
import { ThSortType } from "@patternfly/react-table/dist/esm/components/Table/base/types";

const Columns = [
  "id",
  "status",
  "leader",
  "preferredLeader",
  "replicas",
  "storage",
] as const;
const SortColumns = [
  "id",
  "leader",
  "preferredLeader",
  "status",
  "storage",
] as const;
const StatusLabel: Record<PartitionStatus, { label: ReactNode }> = {
  FullyReplicated: {
    label: (
      <>
        <Icon status={"success"}>
          <CheckCircleIcon />
        </Icon>{" "}
        In-sync
      </>
    ),
  },
  UnderReplicated: {
    label: (
      <>
        <Icon status={"warning"}>
          <ExclamationTriangleIcon />
        </Icon>{" "}
        Under replicated
      </>
    ),
  },
  Offline: {
    label: (
      <>
        <Icon status={"danger"}>
          <ExclamationCircleIcon />
        </Icon>{" "}
        Offline
      </>
    ),
  },
};

export function PartitionsTable({
  topic: initialData,
  kafkaId,
}: {
  kafkaId: string;
  topic: Topic | undefined;
}) {
  const t = useTranslations("topics");

  const { page, perPage, setPagination } = usePagination();

  const [topic, setTopic] = useState(initialData);
  const [filter, setFilter] = useState<"all" | PartitionStatus>("all");
  const [sort, setSort] = useState<{
    sort: (typeof SortColumns)[number];
    dir: "asc" | "desc";
  }>({ sort: "id", dir: "asc" });

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;

    if (initialData) {
      interval = setInterval(async () => {
        const response = await getTopic(kafkaId, initialData.id);

        if (response.errors) {
          console.warn("Failed to reload topic", {
            kafkaId,
            topicId: initialData.id,
          });
        } else {
          setTopic(response.payload!);
        }
      }, 30000);
    }
    return () => clearInterval(interval);
  }, [kafkaId, initialData]);
  const filteredData = topic?.attributes.partitions
    ?.filter((p) => (filter !== "all" ? p.status === filter : true))
    .sort((a, b) => {
      switch (sort.sort) {
        case "id":
          return a.partition - b.partition;
        case "leader":
          return (a.leaderId ?? 0) - (b.leaderId ?? 0);
        case "status":
          return a.status.localeCompare(b.status);
        case "preferredLeader":
          const apl = a.leaderId === a.replicas[0]?.nodeId;
          const bpl = b.leaderId === b.replicas[0]?.nodeId;
          return Number(apl) - Number(bpl);
        case "storage":
          return (a.leaderLocalStorage ?? 0) - (b.leaderLocalStorage ?? 0);
      }
    });
  const sortedData =
    sort.dir === "asc" ? filteredData : filteredData?.reverse();

  const startIndex = (page - 1) * perPage;
  const paginatedData = sortedData?.slice(startIndex, startIndex + perPage);
  const sortProvider = (col: typeof Columns[number]): ThSortType & { label?: string } | undefined => {
    if (col !== "replicas") {
      return {
        columnIndex: SortColumns.indexOf(col),
        label: "",
        onSort: (_x:any, _y:any, dir: "asc" | "desc") => setSort({ sort: col, dir }),
        sortBy: {
          index: SortColumns.indexOf(sort.sort),
          direction: sort.dir,
        },
      };
    }
    return undefined;
  };

  return (
    <PageSection isFilled>
      <TableView
        itemCount={sortedData?.length || 0}
        page={page}
        perPage={perPage}
        onPageChange={(newPage, newPerPage) =>
          setPagination(newPage, newPerPage)
        }
        data={paginatedData}
        emptyStateNoData={<div>{t("partition_table.no_partition")}</div>}
        emptyStateNoResults={
          <NoResultsEmptyState onReset={() => setFilter("all")} />
        }
        isFiltered={filter !== "all"}
        ariaLabel={"Partitions"}
        columns={Columns}
        sortProvider={sortProvider}
        renderHeader={({ column, key }) => {
          const sortAction = sortProvider(column);

          switch (column) {
            case "id":
              return (
                <Th key={key} dataLabel={"Partition Id"} width={15} sort={sortAction}>
                  {t("partition_table.partition_id")}
                </Th>
              );
            case "status":
              return (
                <Th key={key} dataLabel={"Status"} width={15} sort={sortAction}>
                  {t("partition_table.status")}
                </Th>
              );
            case "preferredLeader":
              return (
                <Th key={key} dataLabel={"Preferred leader"} width={20} sort={sortAction}>
                  {t("partition_table.preferred_leader")}{" "}
                  <Tooltip content={t("partition_table.leader_tooltip")}>
                    <HelpIcon />
                  </Tooltip>
                </Th>
              );
            case "leader":
              return (
                <Th key={key} dataLabel={"Leader"} width={15} sort={sortAction}>
                  {t("partition_table.leader")}{" "}
                  <Tooltip
                    style={{ whiteSpace: "pre-line" }}
                    content={t("partition_table.leader_tooltip")}
                  >
                    <HelpIcon />
                  </Tooltip>
                </Th>
              );
            case "replicas":
              return (
                <Th key={key} dataLabel={"Replicas"} width={20} sort={sortAction}>
                  {t("partition_table.replicas")}{" "}
                  <Tooltip content={t("partition_table.replicas_tooltip")}>
                    <HelpIcon />
                  </Tooltip>
                </Th>
              );
            case "storage":
              return (
                <Th key={key} dataLabel={"Storage"} sort={sortProvider("storage")}>
                  {t("partition_table.size")}
                </Th>
              );
          }
        }}
        renderCell={({ row, column, key, Td }) => {
          switch (column) {
            case "id":
              return (
                <Td key={key} dataLabel={"Partition Id"}>
                  {row.partition}
                </Td>
              );
            case "status":
              return (
                <Td key={key} dataLabel={"Status"}>
                  {StatusLabel[row.status].label}
                </Td>
              );
            case "preferredLeader":
              return (
                <Td key={key} dataLabel={"Preferred leader"}>
                  {row.leaderId !== undefined
                    ? row.leaderId === row.replicas[0]?.nodeId
                      ? "Yes"
                      : "No"
                    : "n/a"}
                </Td>
              );
            case "leader":
              const leader = row.replicas.find(
                (r) => r.nodeId === row.leaderId,
              );
              return (
                <Td key={key} dataLabel={"Leader"}>
                  {leader ? (
                    <Tooltip
                      content={
                        <>
                          {t("partition_table.broker_id")}: {leader.nodeId}
                          <br />
                          {t("partition_table.preferred_leader")}
                        </>
                      }
                    >
                      <Label
                        color={"teal"}
                        isCompact={true}
                        icon={<FlagIcon />}
                      >
                        {leader.nodeId}
                      </Label>
                    </Tooltip>
                  ) : (
                    "n/a"
                  )}
                </Td>
              );
            case "replicas":
              return (
                <Td key={key} dataLabel={"Replicas"}>
                  <LabelGroup>
                    {row.replicas
                      .filter((r) => r.nodeId !== row.leaderId)
                      .map((r, idx) => (
                        <Tooltip
                          key={idx}
                          content={
                            <>
                              {t("partition_table.broker_id")}: {r.nodeId}
                              <br />
                              {t("partition_table.replica")}{" "}
                              {r.inSync ? "in-sync" : "under replicated"}
                            </>
                          }
                        >
                          <Label
                            color={!r.inSync ? "red" : undefined}
                            isCompact={true}
                            icon={
                              !r.inSync ? (
                                <Icon status={"warning"}>
                                  <ExclamationTriangleIcon />
                                </Icon>
                              ) : (
                                <Icon status={"success"}>
                                  <CheckCircleIcon />
                                </Icon>
                              )
                            }
                          >
                            {r.nodeId}
                          </Label>
                        </Tooltip>
                      ))}
                  </LabelGroup>
                </Td>
              );
            case "storage":
              return (
                <Td key={key} dataLabel={"Size"}>
                  <Bytes value={row.leaderLocalStorage} />
                </Td>
              );
          }
        }}
        tools={[
          <ToggleGroup key="filter" aria-label="Filter partitions by state">
            <ToggleGroupItem
              text={`All partitions (${topic?.attributes.partitions?.length})`}
              buttonId="all"
              isSelected={filter === "all"}
              onChange={() => {
                setFilter("all");
              }}
            />
            <ToggleGroupItem
              text={
                <>
                  {StatusLabel.FullyReplicated.label} (
                  {topic?.attributes.partitions?.filter(
                    (p) => p.status === "FullyReplicated",
                  ).length || 0}
                  )
                </>
              }
              buttonId="in-sync"
              isSelected={filter === "FullyReplicated"}
              onChange={() => {
                setFilter("FullyReplicated");
              }}
            />
            <ToggleGroupItem
              text={
                <>
                  {StatusLabel.UnderReplicated.label} (
                  {topic?.attributes.partitions?.filter(
                    (p) => p.status === "UnderReplicated",
                  ).length || 0}
                  )
                </>
              }
              buttonId="under-replicated"
              isSelected={filter === "UnderReplicated"}
              onChange={() => {
                setFilter("UnderReplicated");
              }}
            />
            <ToggleGroupItem
              text={
                <>
                  {StatusLabel.Offline.label} (
                  {topic?.attributes.partitions?.filter(
                    (p) => p.status === "Offline",
                  ).length || 0}
                  )
                </>
              }
              buttonId="offline"
              isSelected={filter === "Offline"}
              onChange={() => {
                setFilter("Offline");
              }}
            />
          </ToggleGroup>,
        ]}
      />
    </PageSection>
  );
}
