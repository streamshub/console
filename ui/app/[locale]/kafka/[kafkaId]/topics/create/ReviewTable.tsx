"use client";
import { ConfigMap, NewConfigMap } from "@/api/topics/schema";
import { TableVariant } from "@patternfly/react-table";
import { useCallback, useMemo } from "react";
import {
  ResponsiveTable,
  ResponsiveTableProps,
} from "../../../../../../components/Table";

type Column = "property" | "new-value" | "initial-value";
const columns: readonly Column[] = [
  "property",
  "new-value",
  "initial-value",
] as const;

export function ReviewTable({
  options,
  initialOptions,
}: {
  options: NewConfigMap;
  initialOptions: ConfigMap;
}) {
  const data = useMemo(() => Object.entries(options), [options]);
  const renderCell = useCallback<
    ResponsiveTableProps<(typeof data)[number], Column>["renderCell"]
  >(
    ({ column, key, row: [name, value], Td }) => {
      switch (column) {
        case "property":
          return (
            <Td key={key}>
              <div>{name}</div>
            </Td>
          );
        case "new-value":
          return <Td key={key}>{value.value}</Td>;
        case "initial-value":
          return <Td key={key}>{initialOptions[name]?.value}</Td>;
      }
    },
    [initialOptions],
  );
  return (
    <ResponsiveTable
      ariaLabel={"Topic configuration"}
      columns={columns}
      data={data}
      renderHeader={({ column, Th, key }) => {
        switch (column) {
          case "property":
            return (
              <Th width={40} key={key}>
                Property
              </Th>
            );
          case "new-value":
            return <Th key={key}>New value</Th>;
          case "initial-value":
            return <Th key={key}>Initial value</Th>;
        }
      }}
      renderCell={renderCell}
      variant={TableVariant.compact}
    />
  );
}
