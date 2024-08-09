"use client";

import { ClusterList } from "@/api/kafka/schema";
import { ButtonLink } from "@/components/Navigation/ButtonLink";
import { ResponsiveTable } from "@/components/Table";
import { Truncate } from "@/libs/patternfly/react-core";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";

const columns = ["name", "version", "namespace", "login"] as const;

export function ClustersTable({
  clusters,
}: {
  clusters: ClusterList[] | undefined;
}) {
  const t = useTranslations();
  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      variant={TableVariant.compact}
      columns={columns}
      data={clusters}
      renderHeader={({ column, Th }) => {
        switch (column) {
          case "name":
            return (
              <Th key="name_header" width={25}>
                {t("ClustersTable.name")}
              </Th>
            );
          case "version":
            return (
              <Th key="version_header">{t("ClustersTable.kafka_version")}</Th>
            );
          case "namespace":
            return <Th key="namespace_header">{t("ClustersTable.project")}</Th>;
          case "login":
            return (
              <Th
                key="login_header"
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
                <Truncate content={row.attributes.name} />
              </Td>
            );
          case "version":
            return (
              <Td key={key}>
                {row.attributes.kafkaVersion ?? "Not Available"}
              </Td>
            );
          case "namespace":
            return <Td key={key}>{row.attributes.namespace ?? "N/A"}</Td>;
          case "login":
            return (
              <Td key={key} modifier={"fitContent"}>
                <ButtonLink href={`/kafka/${row.id}/login`} variant={"primary"}>
                  Login to cluster
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
