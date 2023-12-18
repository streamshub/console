"use client";

import { ClusterList } from "@/api/kafka/schema";

import { useOpenClusterConnectionPanel } from "@/app/[locale]/ClusterDrawerContext";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import { Truncate } from "@/libs/patternfly/react-core";
import { CopyIcon, IntegrationIcon } from "@/libs/patternfly/react-icons";
import { TableVariant } from "@/libs/patternfly/react-table";
import { Skeleton } from "@patternfly/react-core";
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
            return <Th width={25}>Name</Th>;
          case "nodes":
            return <Th>Brokers</Th>;
          case "consumers":
            return <Th>Consumer groups</Th>;
          case "version":
            return <Th>Kafka version</Th>;
          case "namespace":
            return <Th>Project</Th>;
        }
      }}
      renderCell={({ key, column, row, Td }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key}>
                <Link href={`/kafka/${row.id}`}>
                  <Truncate content={row.attributes.name} />
                </Link>
              </Td>
            );
          case "nodes":
            return (
              <Td key={key}>
                <Suspense fallback={<Skeleton />}>
                  <NodesCell kafkaId={row.id} data={row.extra.nodes} />
                </Suspense>
              </Td>
            );
          case "consumers":
            return (
              <Td key={key}>
                <Suspense fallback={<Skeleton />}>
                  <ConsumersCell
                    kafkaId={row.id}
                    data={row.extra.consumerGroupsCount}
                  />
                </Suspense>
              </Td>
            );
          case "version":
            return <Td key={key}>{row.attributes.kafkaVersion || "n/a"}</Td>;
          case "namespace":
            return <Td key={key}>{row.attributes.namespace}</Td>;
        }
      }}
      renderActions={({ ActionsColumn, row }) => (
        <ActionsColumn
          items={[
            {
              title: "Connect to this cluster",
              onClick: () => {
                open(row.id);
              },
              icon: <IntegrationIcon />,
            },
            {
              title: "Copy Bootstrap URL",
              onClick: () => {
                navigator.clipboard.writeText(row.attributes.bootstrapServers);
              },
              icon: <CopyIcon />,
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
  const { online, count } = await data;
  return (
    <>
      <Number value={online} />
      /
      <Number value={count} />
      &nbsp;online&nbsp;
      <Link href={`/kafka/${kafkaId}/nodes`}>Brokers</Link>
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
