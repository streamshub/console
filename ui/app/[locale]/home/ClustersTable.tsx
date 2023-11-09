"use client";

import { ClusterDetail } from "@/api/kafka";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import { CopyIcon, IntegrationIcon } from "@patternfly/react-icons";
import { TableVariant } from "@patternfly/react-table";
import Link from "next/link";

const columns = ["name", "nodes", "consumers", "producers"] as const;

export function ClustersTable({
  clusters = [],
}: {
  clusters: ClusterDetail[] | undefined;
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      variant={TableVariant.compact}
      disableAutomaticColumns={true}
      columns={columns}
      data={clusters}
      renderHeader={({ column, Th }) => {
        switch (column) {
          case "name":
            return <Th width={50}>Name</Th>;
          case "nodes":
            return <Th>Online Brokers</Th>;
          case "producers":
            return <Th>Producers</Th>;
          case "consumers":
            return <Th>Consumers</Th>;
        }
      }}
      renderCell={({ column, row, Td }) => {
        switch (column) {
          case "name":
            return (
              <Td>
                <Link href={`/kafka/${row.id}`}>{row.attributes.name}</Link>
              </Td>
            );
          case "nodes":
            return (
              <Td>
                <Number value={row.attributes.nodes.length} />
                /
                <Number value={row.attributes.nodes.length} />
              </Td>
            );
          case "producers":
            return (
              <Td>
                <Number value={0} />
                /
                <Number value={0} />
              </Td>
            );
          case "consumers":
            return (
              <Td>
                <Number value={0} />
                /
                <Number value={0} />
              </Td>
            );
        }
      }}
      renderActions={({ ActionsColumn, row }) => (
        <ActionsColumn
          items={[
            {
              title: "Connect to this cluster",
              onClick: () => {},
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
