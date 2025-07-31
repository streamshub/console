import { ConnectClusters } from "@/api/kafkaConnect/schema";
import { TableView, TableViewProps } from "@/components/Table";
import { useTranslations } from "next-intl";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import Link from "next/link";

export const ConnectClustersTableColumns = [
  "name",
  "version",
  "workers",
] as const;

export type ConnectClustersTableColumn =
  (typeof ConnectClustersTableColumns)[number];

export function ConnectClustersTable({
  connectClusters,
  page,
  perPage,
  total,
  filterName,
  onFilterNameChange,
  onPageChange,
  isColumnSortable,
  onClearAllFilters,
}: {
  connectClusters: ConnectClusters[] | undefined;
  page: number;
  perPage: number;
  total: number;
  filterName: string | undefined;
  onFilterNameChange: (name: string | undefined) => void;
} & Pick<
  TableViewProps<ConnectClusters, ConnectClustersTableColumn>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>) {
  const t = useTranslations("KafkaConnect");

  return (
    <TableView
      data={connectClusters}
      page={page}
      perPage={perPage}
      itemCount={total}
      onPageChange={onPageChange}
      isColumnSortable={isColumnSortable}
      isFiltered={filterName?.length !== 0}
      emptyStateNoData={<></>}
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      ariaLabel={t("connect_clusters_title")}
      columns={ConnectClustersTableColumns}
      onClearAllFilters={onClearAllFilters}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "name":
            return <Th key={key}>{t("connect_clusters.name")}</Th>;
          case "version":
            return <Th key={key}>{t("connect_clusters.version")}</Th>;
          case "workers":
            return <Th key={key}>{t("connect_clusters.workers")}</Th>;
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
          case "version":
            return (
              <Td key={key} dataLabel={t("connectors.connect_cluster")}>
                {row.attributes.version}
              </Td>
            );
          case "workers":
            return (
              <Td key={key} dataLabel={t("connectors.type")}>
                {row.attributes.replicas}
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
