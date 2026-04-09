import { ConfigMap, NewConfigMap } from "@/api/topics/schema";
import { ResponsiveTable, ResponsiveTableProps } from "@/components/Table";
import {
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  TextInput,
} from "@/libs/patternfly/react-core";
import { TableVariant, Th } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";
import { useCallback, useMemo } from "react";

type Column = "property" | "value";
const columns: readonly Column[] = ["property", "value"] as const;

export function ConfigTable({
  options,
  initialOptions,
  onChange,
  fieldError,
}: {
  options: NewConfigMap;
  initialOptions: ConfigMap;
  onChange: (options: NewConfigMap) => void;
  fieldError?: {
    field: string;
    error: string;
  };
}) {
  const t = useTranslations();
  const data = useMemo(() => Object.entries(initialOptions), [initialOptions]);
  const renderCell = useCallback<
    ResponsiveTableProps<(typeof data)[number], Column>["renderCell"]
  >(
    ({ column, key, row: [name, property], Td }) => {
      switch (column) {
        case "property":
          return (
            <Td key={key} dataLabel={t("ConfigTable.property")}>
              <div>{name}</div>
            </Td>
          );
        case "value": {
          const validated = fieldError?.field === name ? "error" : "default";
          return (
            <Td key={name} dataLabel={t("ConfigTable.value")}>
              <FormGroup fieldId={name}>
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
                  validated={validated}
                />
                {validated === "error" && (
                  <FormHelperText>
                    <HelperText>
                      <HelperTextItem variant={validated}>
                        {fieldError?.error}
                      </HelperTextItem>
                    </HelperText>
                  </FormHelperText>
                )}
              </FormGroup>
            </Td>
          );
        }
      }
    },
    [fieldError?.error, fieldError?.field, onChange, options, t],
  );
  return (
    <ResponsiveTable
      ariaLabel={"Topic configuration"}
      columns={columns}
      data={data}
      renderHeader={({ column, key }) => {
        switch (column) {
          case "property":
            return (
              <Th width={40} key={key}>
                {t("ConfigTable.property")}
              </Th>
            );
          case "value":
            return <Th key={key}>{t("ConfigTable.value")}</Th>;
        }
      }}
      renderCell={renderCell}
      variant={TableVariant.compact}
    />
  );
}
