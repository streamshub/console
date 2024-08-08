"use client";

import { ClusterList } from "@/api/kafka/schema";
import { useOpenClusterConnectionPanel } from "@/components/ClusterDrawerContext";
import { ButtonLink } from "@/components/Navigation/ButtonLink";
import { ResponsiveTable } from "@/components/Table";
import { Truncate } from "@/libs/patternfly/react-core";
import { ExternalLinkAltIcon } from "@/libs/patternfly/react-icons";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";
import Link from "next/link";

const columns = ["name", "version", "namespace", "login"] as const;

export function ClustersTable({
  clusters,
}: {
  clusters: ClusterList[] | undefined;
}) {
  const t = useTranslations();
  const open = useOpenClusterConnectionPanel();
  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      variant={TableVariant.compact}
      columns={columns}
      data={clusters}
      renderHeader={({ column, Th }) => {
        switch (column) {
          case "name":
            return <Th width={25}>{t("ClustersTable.name")}</Th>;
          case "version":
            return <Th>{t("ClustersTable.kafka_version")}</Th>;
          case "namespace":
            return <Th>{t("ClustersTable.project")}</Th>;
          case "login":
            return <Th modifier={"fitContent"} />;
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
    />
  );
}
