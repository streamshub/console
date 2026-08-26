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
import { ExclamationTriangleIcon, HelpIcon, SearchIcon } from '@patternfly/react-icons';
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

  const truncate = (value: string | null, emptyText: string): React.ReactNode => {
    if (value === null || value === undefined || value === '') return (
      <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
        {emptyText}
      </span>
    );
    return (
      <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {value}
      </span>
    );
  };

  const isBinaryKey = (message: KafkaRecord) => message.meta?.content?.key?.type === 'application/octet-stream';
  const isBinaryValue = (message: KafkaRecord) => message.meta?.content?.value?.type === 'application/octet-stream';

  const renderHeaders = (headers: Record<string, unknown>, message: KafkaRecord) => {
    const entries = Object.entries(headers);
    if (entries.length === 0) return (
      <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
        {t('topics.messages.noHeaders')}
      </span>
    );
    return (
      <div>
        {entries.map(([k, v]) => {
          const isBinary = message.meta?.content?.headers?.[k]?.type === 'application/octet-stream';
          return (
            <div key={k} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              <strong>{k}</strong>:{' '}
              {isBinary ? (
                <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                  {t('topics.messages.binaryDataNotDisplayed')}
                </span>
              ) : (
                String(v)
              )}
            </div>
          );
        })}
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
        return (
          <>
            {isBinaryKey(message) ? (
              <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                {t('topics.messages.binaryDataNotDisplayed')}
              </span>
            ) : (
              truncate(message.attributes.key, t('topics.messages.noKey'))
            )}
            {message.relationships.keySchema && (
              <Content>
                <Content component="small">
                  {message.relationships.keySchema.meta?.name}
                  {message.relationships.keySchema.meta?.errors && (
                    <> <ExclamationTriangleIcon /> {message.relationships.keySchema.meta.errors[0].detail}</>
                  )}
                </Content>
              </Content>
            )}
          </>
        );
      case 'headers':
        return renderHeaders(message.attributes.headers, message);
      case 'value':
        return (
          <>
            {isBinaryValue(message) ? (
              <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                {t('topics.messages.binaryDataNotDisplayed')}
              </span>
            ) : (
              truncate(message.attributes.value, t('topics.messages.noValue'))
            )}
            {message.relationships.valueSchema && (
              <Content>
                <Content component="small">
                  {message.relationships.valueSchema.meta?.name}
                  {message.relationships.valueSchema.meta?.errors && (
                    <> <ExclamationTriangleIcon /> {message.relationships.valueSchema.meta.errors[0].detail}</>
                  )}
                </Content>
              </Content>
            )}
          </>
        );
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