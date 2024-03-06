import { Button, Modal, Text, TextContent } from "@/libs/patternfly/react-core";
import {
  DataList,
  DataListCell,
  DataListCheck,
  DataListControl,
  DataListItemCells,
} from "@patternfly/react-core";
import { DragDropSort } from "@patternfly/react-drag-drop";
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
  chosenColumns: initialValue,
  onConfirm,
  onCancel,
}: {
  chosenColumns: Column[];
  onConfirm: (columns: Column[]) => void;
  onCancel: () => void;
}) {
  const columnLabels = useColumnLabels();
  const [chosenColumns, setChosenColumns] = useState(initialValue);
  const [sortedColumns, setSortedColumns] = useState(getInitialColumns());

  function getInitialColumns() {
    return [
      ...chosenColumns,
      ...columns.filter((c) => !chosenColumns.includes(c)),
    ];
  }

  function colToDraggable(column: Column) {
    return {
      id: column,
      content: (
        <>
          <DataListControl>
            <DataListCheck
              aria-labelledby={`item-${column}-label`}
              id={`item-${column}`}
              otherControls
              isChecked={chosenColumns.includes(column)}
              onChange={(_, checked) => {
                setChosenColumns((cols) =>
                  checked
                    ? [column, ...cols]
                    : cols.filter((cc) => cc !== column),
                );
              }}
            />
          </DataListControl>
          <DataListItemCells
            dataListCells={[
              <DataListCell key={`item-${column}`}>
                <label id={`item-${column}-label`} htmlFor={`item-${column}`}>
                  {columnLabels[column]}
                </label>
              </DataListCell>,
            ]}
          />
        </>
      ),
    };
  }

  return (
    <Modal
      title="Manage columns"
      isOpen={true}
      variant="small"
      description={
        <TextContent>
          <Text component={"p"}>
            Chosen fields will be displayed in the table.
          </Text>
        </TextContent>
      }
      onClose={onCancel}
      actions={[
        <Button
          key="save"
          variant="primary"
          onClick={() =>
            onConfirm(sortedColumns.filter((c) => chosenColumns.includes(c)))
          }
          isDisabled={chosenColumns.length === 0}
        >
          Save
        </Button>,
        <Button key="cancel" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>,
      ]}
    >
      <DragDropSort
        items={sortedColumns.map(colToDraggable)}
        onDrop={(_, newItems) => {
          setSortedColumns(newItems.map((c) => c.id as Column));
        }}
        variant="DataList"
      >
        <DataList aria-label="Columns" isCompact />
      </DragDropSort>
    </Modal>
  );
}
