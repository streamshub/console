import {
  ConnectorState,
  ConnectorType,
  EnrichedConnector,
} from "@/api/kafkaConnect/schema";
import { TableView, TableViewProps } from "@/components/Table";
import { useTranslations } from "next-intl";
import { Icon, Truncate } from "@/libs/patternfly/react-core";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  HistoryIcon,
  PauseCircleIcon,
  PendingIcon,
} from "@patternfly/react-icons";
import { Th } from "@/libs/patternfly/react-table";
import { ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { ManagedConnectorLabel } from "./ManagedConnectorLabel";

export const ConnectorsTableColumns = [
  "name",
  "connect-cluster",
  "type",
  "state",
  "tasks",
] as const;

export type ConnectorsTableColumn = (typeof ConnectorsTableColumns)[number];

const StateLabel: Record<ConnectorState, { label: ReactNode }> = {
  UNASSIGNED: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp; Unassigned
      </>
    ),
  },
  RUNNING: {
    label: (
      <>
        <Icon status="success">
          <CheckCircleIcon />
        </Icon>
        &nbsp;Running
      </>
    ),
  },
  PAUSED: {
    label: (
      <>
        <Icon>
          <PauseCircleIcon />
        </Icon>
        &nbsp;Paused
      </>
    ),
  },
  STOPPED: {
    label: (
      <>
        <Icon>
          <Image
            src={"/stop-icon.svg"}
            alt="stop icon"
            width={100}
            height={100}
          />
        </Icon>
        &nbsp;Stopped
      </>
    ),
  },
  FAILED: {
    label: (
      <>
        <Icon status="danger">
          <ExclamationCircleIcon />
        </Icon>
        &nbsp;Failed
      </>
    ),
  },
  RESTARTING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Restarting
      </>
    ),
  },
};

const TypeLabel: Record<ConnectorType, { label: ReactNode }> = {
  source: {
    label: <>Source</>,
  },
  sink: {
    label: <>Sink</>,
  },
  "source:mm": {
    label: <>Mirror Source</>,
  },
  "source:mm-checkpoint": {
    label: <>Mirror Checkpoint</>,
  },
  "source:mm-heartbeat": {
    label: <>Mirror Heartbeat</>,
  },
};

export function ConnectorsTable({
  connectors,
  page,
  perPage,
  total,
  filterName,
  onFilterNameChange,
  onPageChange,
  onClearAllFilters,
  kafkaId,
}: {
  connectors: EnrichedConnector[] | undefined;
  page: number;
  perPage: number;
  total: number;
  filterName: string | undefined;
  onFilterNameChange: (name: string | undefined) => void;
  kafkaId: string;
} & Pick<
  TableViewProps<EnrichedConnector, ConnectorsTableColumn>,
  "onPageChange" | "onClearAllFilters"
>) {
  const t = useTranslations("KafkaConnect");

  return (
    <TableView
      data={connectors}
      page={page}
      perPage={perPage}
      itemCount={total}
      onPageChange={onPageChange}
      isFiltered={filterName?.length !== 0}
      onClearAllFilters={onClearAllFilters}
      emptyStateNoData={<></>}
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      ariaLabel={t("connectors_title")}
      columns={ConnectorsTableColumns}
      renderHeader={({ column, key }) => {
        switch (column) {
          case "name":
            return <Th key={key}>{t("connectors.name")}</Th>;
          case "connect-cluster":
            return <Th key={key}>{t("connectors.connect_cluster")}</Th>;
          case "type":
            return <Th key={key}>{t("connectors.type")}</Th>;
          case "state":
            return <Th key={key}>{t("connectors.state")}</Th>;
          case "tasks":
            return <Th key={key}>{t("connectors.tasks")}</Th>;
        }
      }}
      renderCell={({ row, column, key, Td }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={t("connectors.name")}>
                <Link
                  href={`/kafka/${kafkaId}/kafka-connect/${encodeURIComponent(row.id)}`}
                >
                  <Truncate content={row.attributes.name!} />
                </Link>
                {row.meta?.managed === true && <ManagedConnectorLabel />}
              </Td>
            );
          case "connect-cluster":
            return (
              <Td key={key} dataLabel={t("connectors.connect_cluster")}>
                <Link
                  href={`/kafka/${kafkaId}/kafka-connect/connect-clusters/${row.connectClusterId}`}
                >
                  {row.connectClusterName}
                </Link>
              </Td>
            );
          case "type":
            return (
              <Td key={key} dataLabel={t("connectors.type")}>
                {TypeLabel[row.attributes.type].label}
              </Td>
            );
          case "state":
            return (
              <Td key={key} dataLabel={t("connectors.state")}>
                {StateLabel[row.attributes.state]?.label}
              </Td>
            );
          case "tasks":
            return (
              <Td key={key} dataLabel={t("connectors.tasks")}>
                {row.replicas ?? "-"}
              </Td>
            );
        }
      }}
      filters={{
        Name: {
          type: "search",
          chips: filterName ? [filterName] : [],
          onSearch: onFilterNameChange,
          onRemoveChip: () => {
            onFilterNameChange(undefined);
          },
          onRemoveGroup: () => {
            onFilterNameChange(undefined);
          },
          validate: () => true,
          errorMessage: "",
        },
      }}
    />
  );
}
