/**
 * Columns Modal Component
 * Allows users to select and reorder table columns with drag-and-drop
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  Button,
  DataList,
  DataListItemCells,
  DataListCell,
  DataListControl,
  Checkbox,
} from '@patternfly/react-core';
import { DragDropSort, DraggableObject } from '@patternfly/react-drag-drop';

export const columns = [
  'timestampUTC',
  'timestamp',
  'offset-partition',
  'size',
  'key',
  'headers',
  'value',
] as const;

export type Column = (typeof columns)[number];

export function useColumnLabels() {
  const { t } = useTranslation();
  const columnLabels: Record<Column, string> = {
    key: t('topics.messages.columns.key'),
    headers: t('topics.messages.columns.headers'),
    'offset-partition': t('topics.messages.columns.offsetPartition'),
    value: t('topics.messages.columns.value'),
    size: t('topics.messages.columns.size'),
    timestamp: t('topics.messages.columns.timestamp'),
    timestampUTC: t('topics.messages.columns.timestampUTC'),
  };
  return columnLabels;
}

interface ColumnsModalProps {
  chosenColumns: Column[];
  onConfirm: (columns: Column[]) => void;
  onCancel: () => void;
}

export function ColumnsModal({
  chosenColumns: initialValue,
  onConfirm,
  onCancel,
}: ColumnsModalProps) {
  const { t } = useTranslation();
  const columnLabels = useColumnLabels();
  const [chosenColumns, setChosenColumns] = useState<Column[]>(initialValue);
  const [sortedColumns, setSortedColumns] = useState<Column[]>(() => {
    // Start with selected columns in order, then add unselected ones
    return [
      ...initialValue,
      ...columns.filter((c) => !initialValue.includes(c)),
    ];
  });

  const handleToggle = (column: Column, checked: boolean) => {
    setChosenColumns((cols) =>
      checked ? [column, ...cols] : cols.filter((c) => c !== column)
    );
  };

  const handleSave = () => {
    // Only save columns that are checked, in the sorted order
    const orderedSelectedColumns = sortedColumns.filter((c) =>
      chosenColumns.includes(c)
    );
    onConfirm(orderedSelectedColumns);
  };

  const colToDraggable = (column: Column): DraggableObject => ({
    id: column,
    content: (
      <>
        <DataListControl>
          <Checkbox
            aria-labelledby={`item-${column}-label`}
            id={`item-${column}`}
            isChecked={chosenColumns.includes(column)}
            onChange={(_, checked) => handleToggle(column, checked)}
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
  });

  return (
    <Modal
      variant={ModalVariant.small}
      title={t('topics.messages.manage_columns')}
      isOpen={true}
      onClose={onCancel}
    >
      <div style={{ padding: '1.5rem' }}>
        <p style={{ marginBottom: '1rem' }}>
          {t('topics.messages.manage_columns_description')}
        </p>
        <DragDropSort
          items={sortedColumns.map(colToDraggable)}
          onDrop={(_, newItems) => {
            setSortedColumns(newItems.map((item) => item.id as Column));
          }}
          variant="DataList"
        >
          <DataList aria-label={t('topics.messages.columns_list')} isCompact />
        </DragDropSort>
      </div>
      <div style={{ padding: '1.5rem', paddingTop: '0', display: 'flex', gap: '0.5rem' }}>
        <Button
          variant="primary"
          onClick={handleSave}
          isDisabled={chosenColumns.length === 0}
        >
          {t('common.save')}
        </Button>
        <Button variant="link" onClick={onCancel}>
          {t('common.cancel')}
        </Button>
      </div>
    </Modal>
  );
}