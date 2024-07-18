"use client";

import { ClusterList } from "@/api/kafka/schema";

import { useOpenClusterConnectionPanel } from "@/app/[locale]/ClusterDrawerContext";
import { Number } from "@/components/Format/Number";
import { ResponsiveTable } from "@/components/Table";
import { Skeleton, Truncate } from "@/libs/patternfly/react-core";
import { ExternalLinkAltIcon } from "@/libs/patternfly/react-icons";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { Suspense } from "react";

const columns = ["name", "nodes", "consumers", "version", "namespace"] as const;

type AsyncNodesExtra = Promise<{
  online: number;
  count: number;
}>;
type AsyncConsumerExtra = Promise<number>;
export type EnrichedClusterList = ClusterList & {
  extra: {
    nodes: AsyncNodesExtra;
    consumerGroupsCount: AsyncConsumerExtra;
  };
};

export function ClustersTable({
  clusters,
}: {
  clusters: EnrichedClusterList[] | undefined;
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
          case "nodes":
            return <Th>{t("ClustersTable.brokers")}</Th>;
          case "consumers":
            return <Th>{t("ClustersTable.consumer_groups")}</Th>;
          case "version":
            return <Th>{t("ClustersTable.kafka_version")}</Th>;
          case "namespace":
            return <Th>{t("ClustersTable.project")}</Th>;
        }
      }}
      renderCell={({ key, column, row, Td }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key}>
                {row.meta.configured === true ? (
                  <Link href={`/kafka/${row.id}`}>
                    <Truncate content={row.attributes.name} />
                  </Link>
                ) : (
                  <Truncate content={row.attributes.name} />
                )}
              </Td>
            );
          case "nodes":
            return row.meta.configured === true ? (
              <Td key={key}>
                <Suspense fallback={<Skeleton />}>
                  <NodesCell kafkaId={row.id} data={row.extra.nodes} />
                </Suspense>
              </Td>
            ) : (
              <i>{t("ClustersTable.connection_not_configured")}</i>
            );
          case "consumers":
            return row.meta.configured === true ? (
              <Td key={key}>
                <Suspense fallback={<Skeleton />}>
                  <ConsumersCell
                    kafkaId={row.id}
                    data={row.extra.consumerGroupsCount}
                  />
                </Suspense>
              </Td>
            ) : (
              <i>{t("ClustersTable.connection_not_configured")}</i>
            );
          case "version":
            return <Td key={key}>{row.attributes.kafkaVersion ?? "Not Available"}</Td>;
          case "namespace":
            return <Td key={key}>{row.attributes.namespace ?? "N/A"}</Td>;
        }
      }}
      renderActions={({ ActionsColumn, row }) => (
        <ActionsColumn
          items={[
            {
              title: t("ClustersTable.connection_details"),
              onClick: () => {
                open(row.id);
              },
            },
            {
              title: t("ClustersTable.view_openshift_console"),
              icon: <ExternalLinkAltIcon />,
              isDisabled: true,
            },
          ]}
        />
      )}
    />
  );
}

async function NodesCell({
  kafkaId,
  data,
}: {
  kafkaId: string;
  data: AsyncNodesExtra;
}) {
  const t = useTranslations();
  const { online, count } = await data;
  return (
    <>
      <Link href={`/kafka/${kafkaId}/nodes`}>
          <Number value={online} />
          /
          <Number value={count} />
          &nbsp;{t("ClustersTable.online")}
      </Link>
    </>
  );
}

async function ConsumersCell({
  kafkaId,
  data,
}: {
  kafkaId: string;
  data: AsyncConsumerExtra;
}) {
  const count = await data;
  return (
    <>
      <Link href={`/kafka/${kafkaId}/consumer-groups`}>
        <Number value={count} />
      </Link>
    </>
  );
}
