"use client";

import { KafkaNode } from "@/api/kafka/schema";
import { ResponsiveTable } from "@/components/table";
import { Label } from "@patternfly/react-core";
import { ServerIcon } from "@patternfly/react-icons";
import Link from "next/link";

const columns = ["id", "replicas", "rack"] as const;

export function NodesTable({
  nodes,
  controller,
  //metrics,
}: {
  nodes: KafkaNode[];
  controller: KafkaNode;
  //metrics: Record<string, any>;
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      columns={columns}
      data={nodes}
      renderHeader={({ column, Th }) => {
        switch (column) {
          case "id":
            return (
              <Th>
                <ServerIcon />
                &nbsp; Node
              </Th>
            );
          case "replicas":
            return <Th>Total Replicas</Th>;
          case "rack":
            return <Th>Rack</Th>;
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "id":
            return (
              <Td key={key} dataLabel={"Node"}>
                <div>
                  <Link href={`nodes/${row.id}`}>Node {row.id}</Link>
                </div>
                {row.id === controller.id && (
                  <Label color={"purple"} isCompact={true}>
                    Controller
                  </Label>
                )}
              </Td>
            );
          case "replicas":
            return (
              <Td key={key} dataLabel={"Host"}>
                {/*
                { metrics.values.replica_count
                    .find((e: any) => parseInt(e.nodeId) == row.id)?.value ?? "-" }
*/}
                TODO
              </Td>
            );
          case "rack":
            return (
              <Td key={key} dataLabel={"Rack"}>
                {row.rack || "-"}
              </Td>
            );
        }
      }}
    />
  );
}
