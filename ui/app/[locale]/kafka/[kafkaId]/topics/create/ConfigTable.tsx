"use client";
import { ConfigSchemaMap } from "@/api/topics";
import { ResponsiveTable } from "@/components/table";
import { TextInput } from "@patternfly/react-core";
import { TableVariant } from "@patternfly/react-table";

export function ConfigTable({
  options,
  defaultOptions,
}: {
  options: ConfigSchemaMap;
  defaultOptions: ConfigSchemaMap;
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Node configuration"}
      columns={["property", "value"] as const}
      data={Object.entries(defaultOptions)}
      renderHeader={({ column, Th, key }) => {
        switch (column) {
          case "property":
            return (
              <Th width={40} key={key}>
                Property
              </Th>
            );
          case "value":
            return <Th key={key}>Value</Th>;
        }
      }}
      renderCell={({ column, key, row: [name, property], Td }) => {
        switch (column) {
          case "property":
            return (
              <Td key={key}>
                <div>{name}</div>
              </Td>
            );
          case "value":
            return (
              <Td key={key}>
                <TextInput
                  placeholder={property.value}
                  value={options[column]?.value}
                />
              </Td>
            );
        }
      }}
      variant={TableVariant.compact}
    />
  );
}
