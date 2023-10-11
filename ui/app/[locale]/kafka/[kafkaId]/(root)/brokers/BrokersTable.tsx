"use client";

import { KafkaNode } from "@/api/types";
import { ResponsiveTable } from "@/components/table";
import { ClipboardCopy, Label } from "@patternfly/react-core";
import { ServerIcon } from "@patternfly/react-icons";
import Link from "next/link";

const columns = ["id", "host", "rack"] as const;

export function BrokersTable({
  brokers,
  controller,
}: {
  brokers: KafkaNode[];
  controller: KafkaNode;
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      columns={columns}
      data={brokers}
      renderHeader={({ column, Th }) => {
        switch (column) {
          case "id":
            return (
              <Th>
                <ServerIcon />
                &nbsp; Broker
              </Th>
            );
          case "host":
            return <Th>Host</Th>;
          case "rack":
            return <Th>Rack</Th>;
        }
      }}
      renderCell={({ column, row, Td }) => {
        switch (column) {
          case "id":
            return (
              <Td>
                <Link href={`brokers/${row.id}`}>Broker {row.id}</Link>
                {row.id === controller.id && (
                  <Label color={"purple"} isCompact={true}>
                    Controller
                  </Label>
                )}
              </Td>
            );
          case "host":
            return (
              <Td>
                <ClipboardCopy
                  hoverTip="Copy"
                  clickTip="Copied"
                  variant="inline-compact"
                  isBlock={true}
                >
                  {row.host}:{row.port}
                </ClipboardCopy>
              </Td>
            );
          case "rack":
            return <Td>{row.rack || "-"}</Td>;
        }
      }}
    />
  );
}
