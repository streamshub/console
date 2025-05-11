"use client";

import { ClusterList } from "@/api/kafka/schema";
import { ButtonLink } from "@/components/Navigation/ButtonLink";
import { TableView } from "@/components/Table";
import { Truncate } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/routing";

export const ClusterColumns = [
  "name",
  "version",
  "namespace",
  "authentication",
  "login",
] as const;

export type ClusterColumn = (typeof ClusterColumns)[number];

export function ClustersTable({
  clusters,
  clusterCount,
  authenticated,
  onPageChange,
  page,
  perPage,
}: {
  clusters: ClusterList[] | undefined;
  authenticated: boolean;
  page: number;
  perPage: number;
  onPageChange: (page: number, perPage: number) => void;
  clusterCount: number;
}) {
  const t = useTranslations();

  const columns = authenticated
    ? (["name", "version", "namespace"] as const)
    : ClusterColumns;

  return (
    <TableView
      itemCount={clusterCount}
      page={page}
      perPage={perPage}
      onPageChange={onPageChange}
      data={clusters}
      emptyStateNoData={<div>{t("ClustersTable.no_data")}</div>}
      emptyStateNoResults={<div>{t("ClustersTable.no_data")}</div>}
      ariaLabel={"Kafka clusters"}
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
              <Th key={key} modifier="fitContent" aria-label="Login buttons" />
            );
        }
      }}
      renderCell={({ column, row, key, Td }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={t("ClustersTable.name")}>
                {authenticated ? (
                  <Link href={`/kafka/${row.id}`}>
                    <Truncate content={row.attributes.name} />
                  </Link>
                ) : (
                  <Truncate content={row.attributes.name} />
                )}
              </Td>
            );
          case "version":
            return (
              <Td key={key} dataLabel={t("ClustersTable.kafka_version")}>
                {row.attributes.kafkaVersion ??
                  t("ClustersTable.not_available")}
              </Td>
            );
          case "namespace":
            return (
              <Td key={key} dataLabel={t("ClustersTable.project")}>
                {row.attributes.namespace ?? t("ClustersTable.not_available")}
              </Td>
            );
          case "authentication":
            return (
              <Td key={key} dataLabel={t("ClustersTable.authentication")}>
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
              <Td key={key} modifier="fitContent">
                <ButtonLink
                  href={
                    authenticated
                      ? `/kafka/${row.id}`
                      : `/kafka/${row.id}/login`
                  }
                  variant="primary"
                >
                  {authenticated ? "View" : "Login to cluster"}
                </ButtonLink>
              </Td>
            );
        }
      }}
    />
  );
}
