"use client";

import { ClusterList } from "@/api/kafka/schema";
import { ButtonLink } from "@/components/Navigation/ButtonLink";
import { ResponsiveTable } from "@/components/Table";
import { Truncate } from "@/libs/patternfly/react-core";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/routing";

const columns = [
  "name",
  "version",
  "namespace",
  "authentication",
  "login",
];

export function ClustersTable({
  clusters,
  authenticated,
}: {
  clusters: ClusterList[] | undefined;
  authenticated: boolean
}) {
  const t = useTranslations();
  const columns = authenticated ? [
        "name",
        "version",
        "namespace",
    ] as const : [
        "name",
        "version",
        "namespace",
        "authentication",
        "login",
    ] as const;

  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      variant={TableVariant.compact}
      columns={columns}
      data={clusters}
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
          case "name":
            return (
              <Td key={key}>
                {authenticated
                    ? <Link href={`/kafka/${row.id}`}>
                        <Truncate content={row.attributes.name} />
                      </Link>
                    : <Truncate content={row.attributes.name} />
                }
              </Td>
            );
          case "version":
            return (
              <Td key={key}>
                {row.attributes.kafkaVersion ??
                  t("ClustersTable.not_available")}
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
                <ButtonLink href={ authenticated ? `/kafka/${row.id}` : `/kafka/${row.id}/login`} variant={"primary"}>
                  { authenticated ? "View" : "Login to cluster" }
                </ButtonLink>
              </Td>
            );
        }
      }}
      // renderActions={({ ActionsColumn, row }) => (
      //   <ActionsColumn
      //     items={[
      //       {
      //         title: t("ClustersTable.connection_details"),
      //         onClick: () => {
      //           open(row.id);
      //         },
      //       },
      //       {
      //         title: t("ClustersTable.view_openshift_console"),
      //         icon: <ExternalLinkAltIcon />,
      //         isDisabled: true,
      //       },
      //     ]}
      //   />
      // )}
    >
      {t("ClustersTable.no_data")}
    </ResponsiveTable>
  );
}
