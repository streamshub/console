/**
 * Messages Table Component
 * Displays Kafka messages in a virtualized table
 */

import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  Button,
  Title,
  Content,
  Tooltip,
} from '@patternfly/react-core';
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { HelpIcon, SearchIcon } from '@patternfly/react-icons';
import { KafkaRecord } from '@/api/types';
import { Column, useColumnLabels } from './ColumnsModal';
import { formatDateTime } from '@/utils/dateTime';

interface MessagesTableProps {
  messages: KafkaRecord[];
  selectedMessage?: KafkaRecord;
  chosenColumns: Column[];
  hasFilters: boolean;
  onSelectMessage: (message: KafkaRecord) => void;
  onReset: () => void;
  topicName: string;
}

export function MessagesTable({
  messages,
  selectedMessage,
  chosenColumns,
  hasFilters,
  onSelectMessage,
  onReset,
}: MessagesTableProps) {
  const { t } = useTranslation();
  const columnLabels = useColumnLabels();

  const truncate = (value: string | null): React.ReactNode => {
    if (!value) return '-';
    return (
      <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {value}
      </span>
    );
  };

  const renderHeaders = (headers: Record<string, unknown>) => {
    const entries = Object.entries(headers);
    if (entries.length === 0) return '-';
    return (
      <div>
        {entries.map(([k, v]) => (
          <div key={k} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            <strong>{k}</strong>: {String(v)}
          </div>
        ))}
      </div>
    );
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
          {t('topics.messages.noDataTitle')}
        </Title>
        <EmptyStateBody>
          {hasFilters
            ? t('topics.messages.noResultsBody')
            : t('topics.messages.noDataBody')}
        </EmptyStateBody>
        <Button variant="primary" onClick={onReset}>
          {hasFilters
            ? t('topics.messages.noResultsReset')
            : t('topics.messages.noDataRefresh')}
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
            <Content>
              <Content component="small">
                Partition {message.attributes.partition}
              </Content>
            </Content>
          </div>
        );
      case 'timestampUTC':
        return formatTimestampUTC(message.attributes.timestamp);
      case 'timestamp':
        return formatTimestampLocal(message.attributes.timestamp);
      case 'key':
        return truncate(message.attributes.key);
      case 'headers':
        return renderHeaders(message.attributes.headers);
      case 'value':
        return truncate(message.attributes.value);
      case 'size':
        return formatBytes(message.attributes.size);
      default:
        return '-';
    }
  };

  return (
    <>
      <div style={{ flex: 1, overflow: 'auto', minWidth: 0 }}>
        <Table aria-label={t('topics.messages.tableAriaLabel')} variant="compact" style={{ tableLayout: 'fixed', width: '100%' }}>
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
                    {column === 'size' ? (
                      <>
                        {columnLabels[column]}{' '}
                        <Tooltip content={t('topics.messages.tooltip.size')}>
                          <HelpIcon />
                        </Tooltip>
                      </>
                    ) : (
                      columnLabels[column]
                    )}
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
                  >
                    {renderCell(column, message)}
                  </Td>
                ))}
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>
    </>
  );
}