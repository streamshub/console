"use client";

import { KafkaNode } from "@/api/types";
import { ResponsiveTable } from "@/components/table";
import { ClipboardCopy, Label, LabelGroup } from "@patternfly/react-core";
import { ServerIcon } from "@patternfly/react-icons";

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
            return <Th width={15}>Id</Th>;
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
                <LabelGroup>
                  <Label icon={<ServerIcon />} color={"orange"}>
                    {row.id}
                  </Label>
                  {row.id === controller.id && (
                    <Label color={"purple"}>Controller</Label>
                  )}
                </LabelGroup>
              </Td>
            );
          case "host":
            return (
              <Td>
                <ClipboardCopy
                  hoverTip="Copy"
                  clickTip="Copied"
                  variant="inline-compact"
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
