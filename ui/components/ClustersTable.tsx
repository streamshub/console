import { TableVariant } from "@patternfly/react-table";
import { TableView, TableViewProps } from "./Table";
import { ClusterList } from "@/api/kafka/schema";
import { useTranslations } from "next-intl";
import { ButtonLink } from "./Navigation/ButtonLink";
import { Link } from "@/i18n/routing";
import { Tooltip, Truncate } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { EmptyStateNoMatchFound } from "./Table/EmptyStateNoMatchFound";
import { KroxyliciousClusterLabel } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/KroxyliciousClusterLabel";

export const ClusterColumns = [
  "name",
  "version",
  "namespace",
  "authentication",
  "login",
] as const;

export type ClusterTableColumn = (typeof ClusterColumns)[number];

export const SortableColumns = ["name"];

export function ClustersTable({
  clusters,
  authenticated,
  page,
  perPage,
  onPageChange,
  clustersCount,
  filterName,
  onFilterNameChange,
  onClearAllFilters,
  isColumnSortable,
}: {
  clusters: ClusterList[] | undefined;
  authenticated: boolean;
  page: number;
  perPage: number;
  clustersCount: number;
  filterName: string | undefined;
  onFilterNameChange: (name: string | undefined) => void;
} & Pick<
  TableViewProps<ClusterList, (typeof ClusterColumns)[number]>,
  "onPageChange" | "onClearAllFilters" | "isColumnSortable"
>) {
  const t = useTranslations();

  const columns: readonly ClusterTableColumn[] = authenticated
    ? ClusterColumns.slice(0, 3)
    : ClusterColumns;

  return (
    <TableView
      itemCount={clustersCount}
      ariaLabel={"Kafka clusters"}
      page={page}
      perPage={perPage}
      variant={TableVariant.compact}
      onPageChange={onPageChange}
      data={clusters}
      emptyStateNoData={<>{t("ClustersTable.no_data")}</>}
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      isFiltered={filterName !== undefined}
      onClearAllFilters={onClearAllFilters}
      isColumnSortable={isColumnSortable}
      columns={columns}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={25}>
                {t("ClustersTable.name")}
              </Th>
            );
          case "version":
            return <Th key={key}>{t("ClustersTable.kafka_version")}</Th>;
          case "namespace":
            return <Th key={key}>{t("ClustersTable.project")}</Th>;
          case "authentication":
            return <Th key={key}>{t("ClustersTable.authentication")}</Th>;
          case "login":
            return (
              <Th
                key={key}
                modifier={"fitContent"}
                aria-label="Login buttons"
              />
            );
        }
      }}
      renderCell={({ key, column, row, Td }) => {
        switch (column) {
          case "name": {
            const isVirtualCluster =
              row.meta?.kind === "virtualkafkaclusters.kroxylicious.io";

            return (
              <Td key={key}>
                {authenticated ? (
                  <>
                    <Link href={`/kafka/${row.id}`}>
                      <Truncate content={row.attributes.name} />
                    </Link>
                    {isVirtualCluster && <KroxyliciousClusterLabel />}
                  </>
                ) : (
                  <>
                    {row.attributes.name}
                    {isVirtualCluster && <KroxyliciousClusterLabel />}
                  </>
                )}
              </Td>
            );
          }
          case "version":
            return (
              <Td key={key}>
                { row.attributes.kafkaVersion ?
                    <>
                        { row.attributes.kafkaVersion }
                        {!row.meta.managed && <>
                            {" "}
                            <Tooltip content={t("ClustersTable.version_derived")}>
                                <HelpIcon />
                            </Tooltip>
                            </>
                        }
                    </>
                    : t("ClustersTable.not_available")
                }
              </Td>
            );
          case "namespace":
            return (
              <Td key={key}>
                {row.attributes.namespace ?? t("ClustersTable.not_available")}
              </Td>
            );
          case "authentication":
            return (
              <Td key={key}>
                {
                  {
                    basic: t("ClustersTable.authentication_basic"),
                    oauth: t("ClustersTable.authentication_oauth"),
                    anonymous: t("ClustersTable.authentication_anonymous"),
                  }[row.meta.authentication?.method ?? "anonymous"]
                }
              </Td>
            );
          case "login":
            return (
              <Td key={key} modifier={"fitContent"}>
                <ButtonLink
                  href={
                    authenticated
                      ? `/kafka/${row.id}`
                      : `/kafka/${row.id}/login`
                  }
                  variant={"primary"}
                >
                  {authenticated ? "View" : "Login to cluster"}
                </ButtonLink>
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
