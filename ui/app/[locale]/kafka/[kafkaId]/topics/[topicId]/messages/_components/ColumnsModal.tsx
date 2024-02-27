import { Button, Modal, Text, TextContent } from "@/libs/patternfly/react-core";
import {
  DualListSelector,
  DualListSelectorControl,
  DualListSelectorControlsWrapper,
  DualListSelectorList,
  DualListSelectorListItem,
  DualListSelectorPane,
} from "@patternfly/react-core";
import { DragDropSort } from "@patternfly/react-drag-drop";
import {
  AngleDoubleLeftIcon,
  AngleDoubleRightIcon,
  AngleLeftIcon,
  AngleRightIcon,
} from "@patternfly/react-icons";
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

  function colToOption(c: Column) {
    return {
      id: c,
      content: columnLabels[c],
      props: { key: c, isSelected: false },
    };
  }

  const [ignoreNextOptionSelect, setIgnoreNextOptionSelect] = useState(false);
  const [chosenColumns, setChosenColumns] = useState(
    initialValue.map(colToOption),
  );
  const [availableColumns, setAvailableColumns] = useState(
    columns
      .filter((c) => !chosenColumns.find((sc) => sc.id === c))
      .map(colToOption),
  );

  const moveSelected = (fromAvailable: boolean) => {
    const sourceColumns = fromAvailable ? availableColumns : chosenColumns;
    const destinationColumns = fromAvailable ? chosenColumns : availableColumns;
    for (let i = 0; i < sourceColumns.length; i++) {
      const option = sourceColumns[i];
      if (option.props.isSelected) {
        sourceColumns.splice(i, 1);
        destinationColumns.push(option);
        option.props.isSelected = false;
        i--;
      }
    }
    if (fromAvailable) {
      setAvailableColumns([...sourceColumns]);
      setChosenColumns([...destinationColumns]);
    } else {
      setChosenColumns([...sourceColumns]);
      setAvailableColumns([...destinationColumns]);
    }
  };

  const moveAll = (fromAvailable: boolean) => {
    if (fromAvailable) {
      setChosenColumns([...availableColumns, ...chosenColumns]);
      setAvailableColumns([]);
    } else {
      setAvailableColumns([...columns.map(colToOption)]);
      setChosenColumns([]);
    }
  };

  const onOptionSelect = (_event: any, index: number, isChosen: boolean) => {
    if (ignoreNextOptionSelect) {
      setIgnoreNextOptionSelect(false);
      return;
    }
    if (isChosen) {
      const newChosen = [...chosenColumns];
      newChosen[index].props.isSelected =
        !chosenColumns[index].props.isSelected;
      setChosenColumns(newChosen);
    } else {
      const newAvailable = [...availableColumns];
      newAvailable[index].props.isSelected =
        !availableColumns[index].props.isSelected;
      setAvailableColumns(newAvailable);
    }
  };

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
          onClick={() => onConfirm(chosenColumns.map((c) => c.id))}
          isDisabled={chosenColumns.length === 0}
        >
          Save
        </Button>,
        <Button key="cancel" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>,
      ]}
    >
      <DualListSelector>
        <DualListSelectorPane
          title="Available"
          status={`${availableColumns.filter((x) => x.props.isSelected).length} of ${
            availableColumns.length
          } columns selected`}
        >
          <DualListSelectorList>
            {availableColumns.map((c, index) => (
              <DualListSelectorListItem
                key={index}
                isSelected={c.props.isSelected}
                id={`composable-available-option-${c.id}`}
                onOptionSelect={(e) => onOptionSelect(e, index, false)}
              >
                {c.content}
              </DualListSelectorListItem>
            ))}
          </DualListSelectorList>
        </DualListSelectorPane>
        <DualListSelectorControlsWrapper aria-label="Selector controls">
          <DualListSelectorControl
            isDisabled={
              !availableColumns.some((option) => option.props.isSelected)
            }
            onClick={() => moveSelected(true)}
            aria-label="Add selected"
          >
            <AngleRightIcon />
          </DualListSelectorControl>
          <DualListSelectorControl
            isDisabled={availableColumns.length === 0}
            onClick={() => moveAll(true)}
            aria-label="Add all"
          >
            <AngleDoubleRightIcon />
          </DualListSelectorControl>
          <DualListSelectorControl
            isDisabled={chosenColumns.length === 0}
            onClick={() => moveAll(false)}
            aria-label="Remove all"
          >
            <AngleDoubleLeftIcon />
          </DualListSelectorControl>
          <DualListSelectorControl
            onClick={() => moveSelected(false)}
            isDisabled={
              !chosenColumns.some((option) => option.props.isSelected)
            }
            aria-label="Remove selected"
          >
            <AngleLeftIcon />
          </DualListSelectorControl>{" "}
        </DualListSelectorControlsWrapper>
        <DualListSelectorPane
          title="Chosen"
          isChosen
          status={`${chosenColumns.filter((x) => x.props.isSelected).length} of ${chosenColumns.length} columns selected`}
        >
          <DragDropSort
            items={chosenColumns.map((c, index) => ({
              ...c,
              props: {
                key: c.props.key,
                isSelected: c.props.isSelected,
                onOptionSelect: (e: any) => onOptionSelect(e, index, true),
              },
            }))}
            onDrop={(_, newItems) => {
              setChosenColumns(newItems as typeof chosenColumns);
            }}
            variant="DualListSelectorList"
          >
            <DualListSelectorList />
          </DragDropSort>
        </DualListSelectorPane>
      </DualListSelector>
    </Modal>
  );
}
