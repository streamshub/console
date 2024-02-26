import {
  Button,
  DataList,
  DataListCell,
  DataListCheck,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  Text,
  TextContent,
} from "@/libs/patternfly/react-core";
import { useState } from "react";

export const columns = [
  "timestampUTC",
  "timestamp",
  "offset-partition",
  "size",
  "key",
  "headers",
  "value",
] as const;
export type Column = (typeof columns)[number];

export function useColumnLabels() {
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const columnLabels: Record<Column, string> = {
    key: "Key",
    headers: "Headers",
    "offset-partition": "Offset",
    value: "Value",
    size: "Size",
    timestamp: `Timestamp (${timeZone})`,
    timestampUTC: "Timestamp (UTC)",
  };
  return columnLabels;
}

export function ColumnsModal({
  isOpen,
  selectedColumns: initialValue,
  onConfirm,
  onCancel,
}: {
  isOpen: boolean;
  selectedColumns: Column[];
  onConfirm: (columns: Column[]) => void;
  onCancel: () => void;
}) {
  const columnLabels = useColumnLabels();
  const [selectedColumns, setSelectedColumns] = useState(initialValue);

  function selectAllColumns() {
    setSelectedColumns([...columns]);
  }

  return (
    <Modal
      title="Manage columns"
      isOpen={isOpen}
      variant="small"
      description={
        <TextContent>
          <Text component={"p"}>
            Selected fields will be displayed in the table.
          </Text>
          <Button isInline onClick={selectAllColumns} variant="link">
            Select all
          </Button>
        </TextContent>
      }
      onClose={onCancel}
      actions={[
        <Button
          key="save"
          variant="primary"
          onClick={() => onConfirm(selectedColumns)}
          isDisabled={selectedColumns.length === 0}
        >
          Save
        </Button>,
        <Button key="cancel" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>,
      ]}
    >
      <DataList
        aria-label="Table column management"
        id="table-column-management"
        isCompact
      >
        {columns.map((c) => (
          <DataListItem key={c} aria-labelledby={c}>
            <DataListItemRow>
              <DataListCheck
                aria-labelledby={c}
                checked={selectedColumns.includes(c)}
                name={`check-${c}`}
                id={`check-${c}`}
                onChange={() =>
                  setSelectedColumns((sc) => {
                    if (sc.includes(c)) {
                      return sc.filter((cc) => cc !== c);
                    } else {
                      return [...sc, c];
                    }
                  })
                }
              />
              <DataListItemCells
                dataListCells={[
                  <DataListCell id={c} key={c}>
                    <label htmlFor={`check-${c}`}>{columnLabels[c]}</label>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList>
    </Modal>
  );
}
