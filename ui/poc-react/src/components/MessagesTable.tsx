/**
 * Messages Table Component
 * Displays Kafka messages in a virtualized table
 */

import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  Button,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { SearchIcon, ColumnsIcon } from '@patternfly/react-icons';
import { KafkaRecord } from '../api/types';
import { ColumnsModal, Column, useColumnLabels } from './ColumnsModal';
import { formatDateTime } from '../utils/dateTime';

const defaultColumns: Column[] = [
  'offset-partition',
  'timestampUTC',
  'key',
  'headers',
  'value',
  'size',
];

interface MessagesTableProps {
  messages: KafkaRecord[];
  selectedMessage?: KafkaRecord;
  onSelectMessage: (message: KafkaRecord) => void;
  onReset: () => void;
  topicName: string;
  renderToolbarItems?: (columnButton: () => React.ReactNode) => React.ReactNode;
}

export function MessagesTable({
  messages,
  selectedMessage,
  onSelectMessage,
  onReset,
  renderToolbarItems,
}: MessagesTableProps) {
  const { t } = useTranslation();
  const columnLabels = useColumnLabels();
  const [showColumnsModal, setShowColumnsModal] = useState(false);
  const [chosenColumns, setChosenColumns] = useState<Column[]>(defaultColumns);

  // Load saved column preferences from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('message-browser-columns');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          setChosenColumns(parsed);
        }
      } catch {
        // Ignore parse errors
      }
    }
  }, []);

  const handleColumnsConfirm = (columns: Column[]) => {
    setChosenColumns(columns);
    localStorage.setItem('message-browser-columns', JSON.stringify(columns));
    setShowColumnsModal(false);
  };

  const formatValue = (value: string | null, maxLength = 100): string => {
    if (!value) return '-';
    if (value.length <= maxLength) return value;
    return value.substring(0, maxLength) + '...';
  };

  const formatHeaders = (headers: Record<string, unknown>): string => {
    const keys = Object.keys(headers);
    if (keys.length === 0) return '-';
    if (keys.length === 1) return `${keys[0]}: ${headers[keys[0]]}`;
    return `${keys.length} headers`;
  };

  const formatTimestampUTC = (timestamp: string): string => {
    return formatDateTime({
      value: timestamp,
      timeZone: 'UTC',
    });
  };

  const formatTimestampLocal = (timestamp: string): string => {
    return formatDateTime({
      value: timestamp,
    });
  };

  const formatBytes = (bytes?: number): string => {
    if (bytes === undefined || bytes === null) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const isSelected = (message: KafkaRecord): boolean => {
    return (
      selectedMessage !== undefined &&
      selectedMessage.attributes.partition === message.attributes.partition &&
      selectedMessage.attributes.offset === message.attributes.offset
    );
  };

  if (messages.length === 0) {
    return (
      <EmptyState>
        <SearchIcon />
        <Title headingLevel="h2" size="lg">
          {t('topics.messages.noResultsTitle')}
        </Title>
        <EmptyStateBody>{t('topics.messages.noResultsBody')}</EmptyStateBody>
        <Button variant="primary" onClick={onReset}>
          {t('topics.messages.noResultsReset')}
        </Button>
      </EmptyState>
    );
  }

  const renderCell = (column: Column, message: KafkaRecord) => {
    switch (column) {
      case 'offset-partition':
        return (
          <div>
            <strong>{message.attributes.offset}</strong>
            <div style={{ fontSize: '0.875rem', color: 'var(--pf-v5-global--Color--200)' }}>
              Partition {message.attributes.partition}
            </div>
          </div>
        );
      case 'timestampUTC':
        return formatTimestampUTC(message.attributes.timestamp);
      case 'timestamp':
        return formatTimestampLocal(message.attributes.timestamp);
      case 'key':
        return formatValue(message.attributes.key);
      case 'headers':
        return formatHeaders(message.attributes.headers);
      case 'value':
        return formatValue(message.attributes.value);
      case 'size':
        return formatBytes(message.attributes.size);
      default:
        return '-';
    }
  };

  const renderColumnButton = () => {
    const { t } = useTranslation();
    return (
      <Tooltip content={t('topics.messages.manage_columns')}>
        <Button
          variant="plain"
          icon={<ColumnsIcon />}
          onClick={() => setShowColumnsModal(true)}
          aria-label={t('topics.messages.manage_columns')}
        />
      </Tooltip>
    );
  };

  return (
    <>
      {renderToolbarItems && renderToolbarItems(renderColumnButton)}
      <div style={{ flex: 1, overflow: 'auto' }}>
        <Table aria-label={t('topics.messages.tableAriaLabel')} variant="compact">
          <Thead>
            <Tr>
              {chosenColumns.map((column) => {
                // Set fixed widths for columns with consistent data
                let modifier: 'fitContent' | 'nowrap' | 'truncate' | undefined;
                let width: 10 | 15 | 20 | undefined;
                if (column === 'offset-partition') {
                  width = 10;
                  modifier = 'nowrap';
                } else if (column === 'timestampUTC' || column === 'timestamp') {
                  modifier = 'nowrap';
                  width = 15;
                } else if (column === 'size') {
                  modifier = 'nowrap';
                  width = 10;
                } else {
                  modifier = 'truncate';
                }
                // key, headers, value get remaining space (no width set)
                
                return (
                  <Th key={column} modifier={modifier} width={width}>
                    {columnLabels[column]}
                  </Th>
                );
              })}
            </Tr>
          </Thead>
          <Tbody>
            {messages.map((message) => (
              <Tr
                key={`${message.attributes.partition}-${message.attributes.offset}`}
                isSelectable
                isRowSelected={isSelected(message)}
                onRowClick={() => onSelectMessage(message)}
              >
                {chosenColumns.map((column) => (
                  <Td
                    key={column}
                    dataLabel={columnLabels[column]}
                    modifier={column === 'key' || column === 'headers' || column === 'value' ? 'truncate' : undefined}
                  >
                    {renderCell(column, message)}
                  </Td>
                ))}
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>

      {showColumnsModal && (
        <ColumnsModal
          chosenColumns={chosenColumns}
          onConfirm={handleColumnsConfirm}
          onCancel={() => setShowColumnsModal(false)}
        />
      )}
    </>
  );
}