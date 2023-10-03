"use client";

import { KafkaResource } from "@/api/types";
import { ResponsiveTable } from "@/components/table";
import { ClipboardCopy, Label, LabelGroup } from "@patternfly/react-core";
import {
  DataSourceIcon,
  SecurityIcon,
  UserIcon,
} from "@patternfly/react-icons";
import Link from "next/link";

const columns = ["name", "source", "bootstrapUrl", "authentication"] as const;

export function ClustersTable({
  clusters = [],
}: {
  clusters: KafkaResource[] | undefined;
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Kafka clusters"}
      columns={columns}
      data={clusters}
      renderHeader={({ column, Th }) => {
        switch (column) {
          case "name":
            return <Th width={15}>Name</Th>;
          case "bootstrapUrl":
            return <Th>Bootstrap url</Th>;
          case "source":
            return <Th>Source</Th>;
          case "authentication":
            return <Th width={15}>Authentication</Th>;
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
          case "bootstrapUrl":
            return (
              <Td>
                <ClipboardCopy
                  hoverTip="Copy"
                  clickTip="Copied"
                  variant="inline-compact"
                >
                  {row.attributes.bootstrapServer}
                </ClipboardCopy>
              </Td>
            );
          case "source":
            return (
              <Td>
                <Label icon={<DataSourceIcon />} color={"blue"}>
                  {row.attributes.source === "auto" ? "Autodiscovery" : "User"}
                </Label>
              </Td>
            );
          case "authentication":
            return (
              <Td>
                {row.attributes.source === "auto" ? (
                  <Label icon={<SecurityIcon />} color={"purple"}>
                    Automatic
                  </Label>
                ) : (
                  <LabelGroup>
                    <Label icon={<SecurityIcon />} color={"purple"}>
                      {row.attributes.mechanism}
                    </Label>
                    <Label icon={<UserIcon />} color={"gold"}>
                      {row.attributes.principal}
                    </Label>
                  </LabelGroup>
                )}
              </Td>
            );
        }
      }}
    />
  );
}
