import { KafkaUser } from "@/api/kafkaUsers/schema";
import { DateTime } from "@/components/Format/DateTime";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import { TableView, TableViewProps } from "@/components/Table/TableView";
import { Truncate } from "@/libs/patternfly/react-core";
import { Th } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";
import Link from "next/link";

export const KafkaUserColumns = [
  "name",
  "namespace",
  "creationTimestamp",
  "username",
  "authenticationType",
] as const;

export type KafkaUserColumn = (typeof KafkaUserColumns)[number];

export type KafkaUsersTableProps = {
  kafkaId: string;
  kafkaUsers: KafkaUser[] | undefined;
  kafkaUserCount: number;
  page: number;
  perPage: number;
  filterUsername: string | undefined;
  onFilterUsernameChange: (username: string | undefined) => void;
} & Pick<
  TableViewProps<KafkaUser, KafkaUserColumn>,
  "onPageChange" | "onClearAllFilters"
>;

export function KafkaUsersTable({
  kafkaId,
  kafkaUsers,
  kafkaUserCount,
  filterUsername,
  onFilterUsernameChange,
  page,
  perPage,
  onPageChange,
  onClearAllFilters,
}: KafkaUsersTableProps) {
  const t = useTranslations("kafkausers");

  return (
    <TableView
      itemCount={kafkaUserCount}
      page={page}
      perPage={perPage}
      onPageChange={onPageChange}
      data={kafkaUsers}
      emptyStateNoData={<div>{t("no_kafka_users")}</div>}
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      onClearAllFilters={onClearAllFilters}
      ariaLabel={"kafka user table"}
      isFiltered={filterUsername !== undefined && filterUsername !== ""}
      columns={KafkaUserColumns}
      renderHeader={({ column, key }) => {
        switch (column) {
          case "name":
            return <Th key={key}>{t("name")}</Th>;
          case "namespace":
            return <Th key={key}>{t("namespace")}</Th>;
          case "creationTimestamp":
            return <Th key={key}>{t("creationTimestamp")}</Th>;
          case "username":
            return <Th key={key}>{t("username")}</Th>;
          case "authenticationType":
            return <Th key={key}>{t("authentication")}</Th>;
        }
      }}
      renderCell={({ row, column, key, Td }) => {
        switch (column) {
          case "name":
            const canViewDetails =
              row.meta.privileges?.includes("GET") === true;

            return (
              <Td key={key} dataLabel={t("name")}>
                {canViewDetails ? (
                  <Link href={`/kafka/${kafkaId}/kafka-users/${row.id}`}>
                    <Truncate content={row.attributes.name} />
                  </Link>
                ) : (
                  <Truncate content={row.attributes.name} />
                )}
              </Td>
            );

          case "namespace":
            return (
              <Td key={key} dataLabel={t("namespace")}>
                {row.attributes.namespace ?? "n/a"}
              </Td>
            );

          case "creationTimestamp":
            return (
              <Td key={key} dataLabel={t("creationTimestamp")}>
                {row.attributes.creationTimestamp ? (
                  <DateTime value={row.attributes.creationTimestamp} />
                ) : (
                  "-"
                )}
              </Td>
            );

          case "username":
            return (
              <Td key={key} dataLabel={t("username")}>
                {row.attributes.username}
              </Td>
            );

          case "authenticationType":
            return (
              <Td key={key} dataLabel={t("authentication")}>
                {row.attributes.authenticationType}
              </Td>
            );
        }
      }}
      filters={{
        Username: {
          type: "search",
          chips: filterUsername ? [filterUsername] : [],
          onSearch: onFilterUsernameChange,
          onRemoveChip: () => {
            onFilterUsernameChange(undefined);
          },
          onRemoveGroup: () => {
            onFilterUsernameChange(undefined);
          },
          validate: () => true,
          errorMessage: "",
        },
      }}
    />
  );
}
