import { ConfigMap, NewConfigMap } from "@/api/topics";
import { ResponsiveTable, ResponsiveTableProps } from "@/components/table";
import { TextInput } from "@patternfly/react-core";
import { TableVariant } from "@patternfly/react-table";
import { useCallback, useMemo } from "react";

type Column = "property" | "value";
const columns: readonly Column[] = ["property", "value"] as const;

export function ConfigTable({
  options,
  initialOptions,
  onChange,
}: {
  options: NewConfigMap;
  initialOptions: ConfigMap;
  onChange: (options: NewConfigMap) => void;
}) {
  const data = useMemo(() => Object.entries(initialOptions), [initialOptions]);
  const renderCell = useCallback<
    ResponsiveTableProps<(typeof data)[number], Column>["renderCell"]
  >(
    ({ column, key, row: [name, property], Td }) => {
      switch (column) {
        case "property":
          return (
            <Td key={key}>
              <div>{name}</div>
            </Td>
          );
        case "value":
          return (
            <Td key={name}>
              <TextInput
                id={`property-${name}`}
                placeholder={property.value}
                value={options[name]?.value || ""}
                onChange={(_, value) => {
                  if (value.trim() !== "") {
                    onChange({
                      ...options,
                      [name]: { value },
                    });
                  } else {
                    const newOpts = { ...options };
                    delete newOpts[name];
                    onChange(newOpts);
                  }
                }}
              />
            </Td>
          );
      }
    },
    [onChange, options],
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
          case "value":
            return <Th key={key}>Value</Th>;
        }
      }}
      renderCell={renderCell}
      variant={TableVariant.compact}
    />
  );
}
