import { Connectors } from "@/api/kafkaConnect/schema";
import { TableView, TableViewProps } from "@/components/Table";
import { useTranslations } from "next-intl";
import { Label } from "@/libs/patternfly/react-core";
import { ReactElement } from "react";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import Link from "next/link";

export const ConnectorsTableColumns = [
  "name",
  "connect-cluster",
  "type",
  "state",
  "tasks",
] as const;

export type ConnectorsTableColumn = (typeof ConnectorsTableColumns)[number];

export function ConnectorsTable({
  connectors,
  page,
  perPage,
  total,
  onPageChange,
  isColumnSortable,
  onClearAllFilters,
}: {
  connectors: Connectors[] | undefined;
  page: number;
  perPage: number;
  total: number;
} & Pick<
  TableViewProps<Connectors, ConnectorsTableColumn>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>) {
  const t = useTranslations("KafkaConnect");

  return (
    <TableView
      data={connectors}
      page={page}
      perPage={perPage}
      itemCount={total}
      onPageChange={onPageChange}
      isColumnSortable={isColumnSortable}
      emptyStateNoData={<div>{t("connectors.no_data")}</div>}
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      ariaLabel={t("connectors.title")}
      columns={ConnectorsTableColumns}
      renderHeader={({ column, key, Th }) => {
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
                <Link href="/">{row.attributes.name}</Link>
              </Td>
            );
          case "connect-cluster":
            return (
              <Td key={key} dataLabel={t("connectors.connect_cluster")}>
                <Link href="/">{row.attributes.workerId}</Link>
              </Td>
            );
          case "type":
            return (
              <Td key={key} dataLabel={t("connectors.type")}>
                {row.attributes.type}
              </Td>
            );
          case "state":
            return (
              <Td key={key} dataLabel={t("connectors.state")}>
                <Label
                  color={
                    row.attributes.state === "RUNNING" ? "green" : "orange"
                  }
                >
                  {row.attributes.state}
                </Label>
              </Td>
            );
          case "tasks":
            return (
              <Td key={key} dataLabel={t("connectors.tasks")}>
                {row.attributes.trace}
              </Td>
            );
        }
      }}
    />
  );
}
